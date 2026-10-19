package com.jichi.springaialibaba.controller.imageGenerate;

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {

    private final ImageModel imageModel;

    public ImageGenerationController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    // 基础生成：传描述，返回图片 URL
    @GetMapping("/generate")
    public String generateImage(@RequestParam String description) {
        ImageResponse response = imageModel.call(new ImagePrompt(description));
        return response.getResult().getOutput().getUrl();
    }

    // 带参数：指定模型、张数、尺寸
    @GetMapping("/generate-custom")
    public List<String> generateCustomImage(
            @RequestParam String description,
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "1024") int width,
            @RequestParam(defaultValue = "1024") int height) {

        ImageResponse response = imageModel.call(
                new ImagePrompt(
                        description,
                        DashScopeImageOptions.builder()
                                .withModel("wanx2.1-t2i-plus")
                                .withN(count)
                                .withWidth(width)
                                .withHeight(height)
                                .build()
                )
        );

        return response.getResults().stream()
                .map(result -> result.getOutput().getUrl())
                .collect(Collectors.toList());
    }
}