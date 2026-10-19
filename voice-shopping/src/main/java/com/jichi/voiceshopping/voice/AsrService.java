package com.jichi.voiceshopping.voice;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class AsrService {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${voice-shopping.voice.asr-model}")
    private String model;

    /**
     * 一次语音识别会话。
     *
     * @param audioFlow 音频帧流（PCM 16k 16bit mono）
     * @param onPartial 实时回调：(文本, 是否最终结果)
     */
    @SneakyThrows
    public CompletableFuture<String> recognize(
            Flowable<ByteBuffer> audioFlow,
            BiConsumer<String, Boolean> onPartial) {

        CompletableFuture<String> done = new CompletableFuture<>();
        StringBuilder finalText = new StringBuilder();

        RecognitionParam param = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(model)
                .format("pcm")
                .sampleRate(16000)
                .build();

        Recognition recognition = new Recognition();
        Flowable<RecognitionResult> result = recognition.streamCall(param, audioFlow);

        Disposable sub = result.subscribe(
                r -> {
                    String text = r.getSentence().getText();
                    boolean isEnd = r.isSentenceEnd();
                    onPartial.accept(text, isEnd);
                    if (isEnd) finalText.append(text);
                },
                err -> {
                    log.error("ASR 错误", err);
                    done.completeExceptionally(err);
                },
                () -> done.complete(finalText.toString())
        );
        done.whenComplete((v, t) -> sub.dispose());
        return done;
    }
}