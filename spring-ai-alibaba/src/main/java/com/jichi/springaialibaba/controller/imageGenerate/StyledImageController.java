package com.jichi.springaialibaba.controller.imageGenerate;

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/styled-image")
public class StyledImageController {

    private final ImageModel imageModel;

    private static final Map<String, String> STYLE_KEYWORDS = Map.of(
            "写实摄影", "professional photography, realistic, high resolution, 8k",
            "扁平插画", "flat design, minimalist illustration, vector style",
            "油画风格", "oil painting style, artistic, textured brushstrokes",
            "水彩风格", "watercolor painting, soft colors, artistic",
            "3D渲染", "3D rendering, CGI, photorealistic, studio lighting",
            "日系动漫", "anime style, Japanese illustration, clean lines",
            "商务简约", "clean corporate style, white background, professional"
    );

    public StyledImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @GetMapping("/generate")
    public String generateStyledImage(
            @RequestParam String description,
            @RequestParam(defaultValue = "写实摄影") String style) {

        String styleKeyword = STYLE_KEYWORDS.getOrDefault(style, "");
        String fullPrompt = description + ", " + styleKeyword;

        return imageModel.call(
                        new ImagePrompt(fullPrompt, DashScopeImageOptions.builder()
                                .withModel("wanx2.1-t2i-plus")
                                .withN(1)
                                .withWidth(1024)
                                .withHeight(1024)
                                .build()))
                .getResult().getOutput().getUrl();
    }
}