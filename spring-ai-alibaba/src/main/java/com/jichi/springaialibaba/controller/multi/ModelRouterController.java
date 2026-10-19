package com.jichi.springaialibaba.controller.multi;

import com.jichi.springaialibaba.service.ModelRouterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/router")
public class ModelRouterController {

    private final ModelRouterService modelRouterService;

    public ModelRouterController(ModelRouterService modelRouterService) {
        this.modelRouterService = modelRouterService;
    }

    /**
     * 按业务场景路由到合适的模型。
     * scene 可选值：code-review / chinese-text / customer-service
     * 不传 scene 默认用 chinese-text
     */
    @GetMapping("/scene")
    public String chatByScene(
            @RequestParam String message,
            @RequestParam(defaultValue = "chinese-text") String scene) {

        // selectForScene 只返回 ChatClient，在这里发起调用
        return modelRouterService.selectForScene(scene)
                .prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 带自动降级的调用。
     * provider 可选值：deepseek / qwen，不传默认用配置文件里的 default-provider
     * 如果指定的模型失败，自动切换到另一个，业务层无感知
     */
    @GetMapping("/fallback")
    public String chatWithFallback(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepseek") String provider) {

        return modelRouterService.callWithFallback(message, provider);
    }
}