package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.dto.Channel;
import com.jichi.voiceshopping.dto.SessionScope;
import com.jichi.voiceshopping.dto.StartSessionRequest;
import com.jichi.voiceshopping.entity.ProductEntity;
import com.jichi.voiceshopping.repository.ProductRepository;
import com.jichi.voiceshopping.security.CurrentUser;
import com.jichi.voiceshopping.service.SessionScopeCache;
import com.jichi.voiceshopping.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final CurrentUser currentUser;
    private final ProductRepository productRepo;
    private final SessionService sessionService;
    private final SessionScopeCache scopeCache;

    @PostMapping("/start")
    public SessionScope startSession(@RequestBody StartSessionRequest req) {
        Long userId = currentUser.id();
        List<Long> allowed = null;

        if (req.channel() == Channel.PRODUCT_PAGE) {
            if (req.boundProductId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "PRODUCT_PAGE 必须带 boundProductId");
            }
            ProductEntity p = productRepo.findById(req.boundProductId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "商品不存在"));
            allowed = List.of(p.getMerchantId());
        } else if (req.channel() == Channel.MERCHANT_HOME) {
            if (req.merchantId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "MERCHANT_HOME 必须带 merchantId");
            }
            allowed = List.of(req.merchantId());
        }
        // HOME_ENTRY / SEARCH_FALLBACK → allowed = null，走全平台

        // 顺带把 session 表落地
        sessionService.openIfAbsent(req.sessionId(), userId, req.channel().name());

        SessionScope scope = new SessionScope(userId, allowed, req.boundProductId());
        scopeCache.put(req.sessionId(), scope);
        return scope;
    }
}