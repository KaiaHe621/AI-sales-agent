package com.jichi.prompt.controller;

import com.jichi.prompt.entity.CustomerServiceConfig;
import com.jichi.prompt.service.CustomerServiceFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-service")
public class CustomerServiceFactoryController {

    private final CustomerServiceFactory customerServiceFactory;

    public CustomerServiceFactoryController(CustomerServiceFactory customerServiceFactory) {
        this.customerServiceFactory = customerServiceFactory;
    }

    @PostMapping("/chat")
    public String chat(@RequestParam String message) {
        CustomerServiceConfig config = new CustomerServiceConfig(
                "鸡翅商城",
                "小鸡",
                List.of("商品咨询", "订单查询", "售后服务"),
                List.of("退款纠纷", "法律问题"),
                "专业友好"
        );

        ChatClient client = customerServiceFactory.createForTenant(config);
        return client.prompt().user(message).call().content();
    }
}