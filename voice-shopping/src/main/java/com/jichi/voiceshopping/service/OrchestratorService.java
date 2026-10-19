package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.compliance.ComplianceChecker;
import com.jichi.voiceshopping.dto.*;
import com.jichi.voiceshopping.entity.OrderEntity;
import com.jichi.voiceshopping.entity.ProductEntity;
import com.jichi.voiceshopping.entity.SessionStateEntity;
import com.jichi.voiceshopping.entity.StreamChunk;
import com.jichi.voiceshopping.event.VoiceEventPublisher;
import com.jichi.voiceshopping.memory.ShortTermMemory;
import com.jichi.voiceshopping.memory.TurnSummarizer;
import com.jichi.voiceshopping.repository.ProductRepository;
import com.jichi.voiceshopping.voice.SentenceAggregator;
import com.jichi.voiceshopping.voice.TtsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final IntentService intentService;
    private final ClarifyService clarifyService;
    private final ParallelRecommendService recommendService;
    private final EmotionService emotionService;
    private final PerspectiveHubService perspectiveHub;
    private final SessionStateService stateService;
    private final SessionService sessionService;
    private final ShortTermMemory memory;
    private final VoiceEventPublisher eventPublisher;
    private final ComplianceChecker compliance;
    private final MeterRegistry metrics;
    private final ProductRepository productRepo;
    private final TurnSummarizer turnSummarizer;
    private final PendingOrderStore pendingStore;
    private final OrderReferenceResolver referenceResolver;
    private final OrderService orderService;
    private final EmotionStreamingService emotionStreamingService;
    private final TtsService tts;
    private final CachedTtsPhrases ttsCache;

    @Value("${voice-shopping.perspective.enabled:false}")
    private boolean perspectiveEnabled;

    /**
     * 处理一次用户输入的主入口。
     *
     * @return 封装好的结果（speechText + displayBlocks），交给 TTS 下发
     */
    public EmotionResult handle(String sessionId, Long userId, String utterance) {

        Timer.Sample sample = Timer.start(metrics);
        try {
            // 幂等开启 session 主表（session_state 外键依赖，首轮必须先创建）
            sessionService.openIfAbsent(sessionId, userId, "HOME_ENTRY");

            eventPublisher.publishUserSpoken(sessionId, userId, utterance);
            //memory.append(sessionId, new ShortTermMemory.Turn("USER", null, utterance, System.currentTimeMillis()));

            SessionStateEntity state = stateService.load(sessionId);

            // 短路：ORDER_CONFIRM 状态直接走订单分支，不跑 IntentAgent
            if ("ORDER_CONFIRM".equals(state.getPhase())) {
                return handleOrderConfirm(sessionId, userId, state, utterance);
            }

            IntentResult intent = intentService.classify(sessionId, utterance);
            log.info("[Orc] sessionId={} intent={} slots={}", sessionId, intent.intent(), intent.slots());

            // 意图兜底矫正：LLM 对"便宜点/换一款"这类价格方向表达经常判成 CLARIFY_NEEDED，
            // 但只要上一轮已经推了商品 + 本轮抽到了 priceDirection，就强制按 PRODUCT_COMPARE 处理
            intent = reviseIntentByContext(state, intent);

            EmotionResult result = switch (intent.intent()) {
                case PRODUCT_RECOMMENDATION -> handleRecommendation(sessionId, userId, state, utterance, intent);
                case CLARIFY_NEEDED -> handleClarify(sessionId, state, utterance, intent);
                case PRODUCT_COMPARE -> handleCompare(sessionId, userId, state, utterance, intent);
                case ORDER_CONFIRM -> handleOrderConfirm(sessionId, userId, state, utterance);
                case CHITCHAT -> handleChitchat(sessionId, userId, utterance);
                case OUT_OF_SCOPE -> handleOutOfScope(sessionId, utterance);
            };

            // 合规兜底
            result = compliance.ensureCompliant(sessionId, userId, result);

            String summary = turnSummarizer.summarize(utterance, intent.intent(), result.speechText());
            memory.append(sessionId, new ShortTermMemory.Turn(
                    "TURN", intent.intent().name(), summary, System.currentTimeMillis()));
//            memory.append(sessionId, new ShortTermMemory.Turn(
//                    "ASSISTANT", intent.intent().name(), result.speechText(), System.currentTimeMillis()));
            stateService.save(state);
            return result;

        } finally {
            sample.stop(metrics.timer("voice_shopping.orchestrator.latency"));
        }
    }

    /* ========== 四条分支的具体实现 ========== */

    private EmotionResult handleRecommendation(String sessionId, Long userId,
                                               SessionStateEntity state,
                                               String utterance, IntentResult intent) {
        // 合并历史槽位 + 本轮新槽位
        Map<String, Object> slots = new HashMap<>();
        if (state.getSlots() != null) slots.putAll(state.getSlots());
        intent.slots().forEach((k, v) -> {
            if (v != null) slots.put(k, v);
        });

        state.setSlots(slots);
        state.setCurrentIntent("PRODUCT_RECOMMENDATION");

        // 澄清判断
        ClarifyResult clarify = clarifyService.decide(sessionId, utterance, slots);
        if (clarify.action() == ClarifyResult.Action.ASK) {
            state.setPhase("CLARIFY");
            state.setPendingAsk(clarify.missingSlots().get(0));
            return new EmotionResult(clarify.questionToAsk(), List.of());
        }

        // 推荐
        state.setPhase("RECOMMEND");
        RecommendResult rec = recommendService.recommend(sessionId, userId, utterance, slots);
        state.setLastRecommendations(rec.items().stream().map(RecommendedItem::productId).toList());

        // 旁路：多视角点评团（15 节 PerspectiveHubService）。失败/关闭时降级为原始 utterance
        String contextForEmotion = utterance;
        if (perspectiveEnabled && !rec.items().isEmpty()) {
            String digest = perspectiveHub.discuss(sessionId, utterance, rec.items());
            if (digest != null && !digest.isBlank()) {
                contextForEmotion = utterance + "\n\n【点评团意见】\n" + digest;
            }
        }

        String userNeeds = formatUserNeeds(slots);
        return emotionService.wrap(sessionId, contextForEmotion, userNeeds, rec);
    }

    private EmotionResult handleClarify(String sessionId, SessionStateEntity state,
                                        String utterance, IntentResult intent) {
        // 用户第一次就说得很模糊（"最近想买点东西"），用意图里抽到的 slots 开始
        Map<String, Object> slots = new HashMap<>(intent.slots());
        state.setSlots(slots);
        state.setPhase("CLARIFY");

        ClarifyResult clarify = clarifyService.decide(sessionId, utterance, slots);
        return new EmotionResult(
                clarify.action() == ClarifyResult.Action.ASK
                        ? clarify.questionToAsk()
                        : "好，我这就帮你挑。",
                List.of());
    }

    private EmotionResult handleCompare(String sessionId, Long userId,
                                        SessionStateEntity state,
                                        String utterance, IntentResult intent) {
        // 对比类 = 在上一次推荐结果基础上，根据当前诉求重新排序/过滤
        Map<String, Object> slots = new HashMap<>();
        if (state.getSlots() != null) slots.putAll(state.getSlots());
        intent.slots().forEach((k, v) -> {
            if (v != null) slots.put(k, v);
        });

        // "便宜点/贵点"要以**上一轮推荐的实际价格**为锚，不能直接用 budget——
        // budget 只是用户给的上限，上一轮推出来的商品可能远低于 budget，
        // 若直接按 budget * 0.8 下调，新上限可能还比上一轮最高价高，起不到"便宜"的效果。
        String pd = (String) slots.get("priceDirection");
        List<Long> lastIds = state.getLastRecommendations();
        if (pd != null && lastIds != null && !lastIds.isEmpty()) {
            List<BigDecimal> lastPrices = productRepo.findByIdIn(lastIds).stream()
                    .map(ProductEntity::getPrice).toList();
            if (!lastPrices.isEmpty()) {
                BigDecimal maxP = lastPrices.stream().max(BigDecimal::compareTo).get();
                BigDecimal minP = lastPrices.stream().min(BigDecimal::compareTo).get();
                // cheaper：新上限 = 上轮最高价 * 0.8，保证比上一轮任何一款都便宜
                // expensive：新下限 = 上轮最低价 * 1.2（这里先放 budget 里，推荐层读 priceMin 字段）
                if ("cheaper".equals(pd)) {
                    slots.put("budget", maxP.multiply(BigDecimal.valueOf(0.8)).intValue());
                } else if ("expensive".equals(pd)) {
                    slots.put("priceMin", minP.multiply(BigDecimal.valueOf(1.2)).intValue());
                }
                // 排除上轮已推过的，避免重复推同一款
                slots.put("excludeProductIds", lastIds);
            }
        }

        RecommendResult rec = recommendService.recommend(sessionId, userId, utterance, slots);
        String userNeeds = formatUserNeeds(slots);
        return emotionService.wrap(sessionId, utterance, userNeeds, rec);
    }

    private EmotionResult handleOrderConfirm(String sessionId, Long userId,
                                             SessionStateEntity state, String utterance) {
        // 已经有 pending 单了，判断是 YES 还是 NO
        PendingOrderStore.PendingOrder pending = pendingStore.get(sessionId);
        if (pending != null) {
            if (containsYes(utterance)) {
                OrderEntity order = orderService.confirm(sessionId);
                state.setPhase("ENDED");
                return new EmotionResult(
                        String.format("下单成功，订单尾号 %s，1-2 天送达。还有想看的吗？",
                                order.getOrderNo().substring(0, 6)),
                        List.of());
            }
            if (containsNo(utterance)) {
                orderService.cancel(sessionId);
                state.setPhase("RECOMMEND");
                return new EmotionResult("好的，鸡哥没给你下。想再聊点别的还是换款看看？", List.of());
            }
            return new EmotionResult("那你是确认要这款还是不要？", List.of());
        }

        // 没有 pending，新起一个
        Optional<Long> pidOpt = referenceResolver.resolve(state, utterance);
        if (pidOpt.isEmpty()) {
            return new EmotionResult(
                    "你想要的是刚才推荐的哪一款？可以说第一款、第二款或者商品名。",
                    List.of());
        }
        PendingOrderStore.PendingOrder po = orderService.preview(sessionId, userId, pidOpt.get(), 1);
        state.setPhase("ORDER_CONFIRM");
        return new EmotionResult(
                String.format("好，帮你准备下单：%s，¥%s，一共 %s 元。确认下单吗？",
                        po.productName(), po.unitPrice(), po.totalAmount()),
                List.of());
    }

    private boolean containsYes(String s) {
        return s != null && (s.contains("确认") || s.contains("可以") || s.contains("就这")
                || s.contains("对") || s.contains("好") || s.contains("嗯"));
    }

    private boolean containsNo(String s) {
        return s != null && (s.contains("不要") || s.contains("算了") || s.contains("取消")
                || s.contains("再想想") || s.contains("等下"));
    }

    private EmotionResult handleChitchat(String sessionId, Long userId, String utterance) {
        // 闲聊走 EmotionService 的闲聊模式（EmotionAgent 内部可判断 products 为空）
        return emotionService.wrap(sessionId, utterance, "",
                new RecommendResult(List.of(), "chitchat"));
    }

    private EmotionResult handleOutOfScope(String sessionId, String utterance) {
        return new EmotionResult(
                "鸡哥现在只负责帮你挑商品，这个问题回头可以找客服处理哈。我们继续聊想买什么？",
                List.of());
    }


    /**
     * 意图后处理：LLM 在两类场景下容易判错，这里做兜底矫正。
     * <p>
     * 1) 上下文比较：上一轮已有推荐 + 本轮抽到 priceDirection →
     * CLARIFY_NEEDED / PRODUCT_RECOMMENDATION 统一改写成 PRODUCT_COMPARE
     * <p>
     * 2) 信息已足：state.slots + 本轮 slots 合并后，category + (budget|scenario|brand)
     * 任一组合齐全，就不该再 CLARIFY_NEEDED，强制改写为 PRODUCT_RECOMMENDATION
     */
    private IntentResult reviseIntentByContext(SessionStateEntity state, IntentResult intent) {
        Map<String, Object> curSlots = intent.slots() == null ? Map.of() : intent.slots();

        // ① 上下文价格对比
        boolean hasLastRecommend = "RECOMMEND".equals(state.getPhase())
                && state.getLastRecommendations() != null
                && !state.getLastRecommendations().isEmpty();
        boolean hasPriceDirection = curSlots.get("priceDirection") != null;
        if (hasLastRecommend && hasPriceDirection
                && (intent.intent() == Intent.CLARIFY_NEEDED
                || intent.intent() == Intent.PRODUCT_RECOMMENDATION)) {
            log.info("[Orc] 意图矫正 {} -> PRODUCT_COMPARE, priceDirection={}",
                    intent.intent(), curSlots.get("priceDirection"));
            return new IntentResult(Intent.PRODUCT_COMPARE, curSlots, intent.confidence());
        }

        // ② 信息已足阈值：合并历史 slots + 本轮 slots
        if (intent.intent() == Intent.CLARIFY_NEEDED) {
            Map<String, Object> merged = new HashMap<>();
            if (state.getSlots() != null) merged.putAll(state.getSlots());
            curSlots.forEach((k, v) -> {
                if (v != null) merged.put(k, v);
            });

            boolean hasCategory = merged.get("category") != null;
            boolean hasAnyAnchor = merged.get("budget") != null
                    || merged.get("scenario") != null
                    || merged.get("brand") != null;
            if (hasCategory && hasAnyAnchor) {
                log.info("[Orc] 意图矫正 CLARIFY_NEEDED -> PRODUCT_RECOMMENDATION, mergedSlots={}", merged);
                return new IntentResult(Intent.PRODUCT_RECOMMENDATION, merged, intent.confidence());
            }
        }

        return intent;
    }

    private static String formatUserNeeds(Map<String, Object> slots) {
        if (slots == null || slots.isEmpty()) return "";
        return slots.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("，"));
    }

    public Flux<StreamChunk> streamHandle(String sessionId, Long userId, String utterance) {
        IntentResult intent = intentService.classify(sessionId, utterance);
        log.info("[Stream] sessionId={} intent={} slots={}",
                sessionId, intent.intent(), intent.slots());

        if (intent.intent() != Intent.PRODUCT_RECOMMENDATION) {
            EmotionResult r = handle(sessionId, userId, utterance);
            return Flux.concat(
                    Flux.just(StreamChunk.text(r.speechText())),
                    ttsAudio(r.speechText()).map(StreamChunk::audio)
            );
        }

        Map<String, Object> slots = intent.slots() == null ? Map.of() : intent.slots();
        RecommendResult rec = recommendService.recommend(sessionId, userId, utterance, slots);
        log.info("[Stream-Rec] sessionId={} slotsForRecommend={} recCount={}",
                sessionId, slots, rec.items().size());

        // 先把商品卡片发下去（用户立刻看到 UI）
        Flux<StreamChunk> productsFlow = Flux.just(StreamChunk.products(rec.items()));

        // EmotionAgent 流式文本 → 句子聚合 → TTS 流式合成
        Flux<String> rawTokens = emotionStreamingService.streamWrap(sessionId, utterance, rec);
        Flux<String> sentences = SentenceAggregator.aggregate(rawTokens);

        Flux<StreamChunk> textFlow = sentences.concatMap(sentence -> Flux.merge(
                Flux.just(StreamChunk.text(sentence)),
                ttsAudio(sentence).map(StreamChunk::audio)
        ));

        return Flux.concat(productsFlow, textFlow);
    }


//    private Flux<ByteBuffer> ttsAudio(String text) {
//        Flowable<ByteBuffer> flow = tts.synthesize(Flowable.just(text));
//        return Flux.from(flow);
//    }

    private Flux<ByteBuffer> ttsAudio(String text) {
        byte[] cached = ttsCache.get(text);
        if (cached != null) {
            log.debug("[Cost] TTS 命中缓存 text={} bytes={}", text, cached.length);
            return Flux.just(java.nio.ByteBuffer.wrap(cached));
        }
        Flowable<ByteBuffer> flow = tts.synthesize(Flowable.just(text));
        return Flux.from(flow);
    }

}