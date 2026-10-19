package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.OrderEntity;
import com.jichi.voiceshopping.entity.ProductEntity;
import com.jichi.voiceshopping.repository.OrderRepository;
import com.jichi.voiceshopping.repository.ProductRepository;
import com.jichi.voiceshopping.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CurrentUser currentUser;
    private final ProductRepository productRepo;
    private final PendingOrderStore pendingStore;
    private final LongTermMemoryWriter longTermMemoryWriter;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 我的某一笔订单详情；查不到或不是自己的，一律 404 不泄露存在性
     */
    public OrderEntity getForUser(Long orderId) {
        return orderRepo.findByIdAndUserId(orderId, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "订单不存在或无权访问"));
    }

    /**
     * 我的所有订单（按时间倒序）
     */
    public List<OrderEntity> listMine() {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(currentUser.id());
    }

    /**
     * 商家后台查自家订单。本节先允许"传什么查什么"，
     * 真正的商家身份鉴权后续章节再补。
     */
    public List<OrderEntity> listForMerchant(Long merchantId) {
        return orderRepo.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }


    /**
     * 生成预览单，短时占用一件库存（Redis 占用，不真减）。
     */
    public PendingOrderStore.PendingOrder preview(String sessionId, Long userId, Long productId, int qty) {
        ProductEntity p = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (p.getStock() < qty) {
            throw new IllegalStateException("库存不足");
        }

        PendingOrderStore.PendingOrder po = new PendingOrderStore.PendingOrder(
                sessionId, userId, p.getMerchantId(), p.getId(), p.getName(),
                p.getSkuCode(), qty, p.getPrice(),
                p.getPrice().multiply(BigDecimal.valueOf(qty)));
        pendingStore.put(po);
        return po;
    }

    /**
     * 用户确认后真正写单。
     */
    @Transactional
    public OrderEntity confirm(String sessionId) {
        PendingOrderStore.PendingOrder po = pendingStore.get(sessionId);
        if (po == null) throw new IllegalStateException("没有待确认订单，或已过期");

        ProductEntity p = productRepo.findById(po.productId())
                .orElseThrow(() -> new IllegalStateException("商品不存在"));
        if (p.getStock() < po.quantity()) throw new IllegalStateException("库存不足");
        p.setStock(p.getStock() - po.quantity());
        productRepo.save(p);

        OrderEntity o = new OrderEntity();
        o.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        o.setUserId(po.userId());
        o.setMerchantId(po.merchantId());
        o.setSessionId(sessionId);
        o.setProductId(po.productId());
        o.setSkuCode(po.skuCode());
        o.setQuantity(po.quantity());
        o.setUnitPrice(po.unitPrice());
        o.setTotalAmount(po.totalAmount());
        o.setStatus("PAID");
        o.setAgentAttribution(true);
        OrderEntity saved = orderRepo.save(o);

        pendingStore.remove(sessionId);

        eventPublisher.publishEvent(new UserBehaviorSink.ProductPurchasedEvent(
                saved.getUserId(),
                saved.getProductId(),
                p.getCategoryL2(),                    // 复用上面 findById 拿到的 p
                saved.getTotalAmount().doubleValue()));

        // 下单成功 = 一次会话的"有效结束"，触发 17 节定义的长期记忆回流：
        // 本次会话的品类/品牌偏好、价格敏感度等信号写回 user_profile_dynamic，
        // @Async 方法不会阻塞下单响应
        longTermMemoryWriter.flushOnSessionEnd(sessionId, po.userId());

        return saved;
    }

    public void cancel(String sessionId) {
        pendingStore.remove(sessionId);
    }

}