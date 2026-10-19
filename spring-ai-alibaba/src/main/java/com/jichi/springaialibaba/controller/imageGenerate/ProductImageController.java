package com.jichi.springaialibaba.controller.imageGenerate;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-image")
public class ProductImageController {

    private final ChatClient chatClient;
    private final ImageModel imageModel;

    public ProductImageController(DashScopeChatModel dashScopeChatModel,
                                  ImageModel imageModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel).build();
        this.imageModel = imageModel;
    }

    record GenerateRequest(
            String productName,
            String productDescription,
            String style     // 写实/卡通/简约/商务
    ) {
    }

    record GenerateResult(
            String imagePrompt,   // 生成图片用的 prompt（方便调试）
            String imageUrl       // 生成的图片 URL
    ) {
    }

    @PostMapping("/generate")
    public GenerateResult generateProductImage(@RequestBody GenerateRequest request) {
        // 第一步：ChatClient 把商品信息转成专业的英文绘画 Prompt
        String imagePrompt = chatClient.prompt()
                .system("""
                        你是一个专业的 AI 绘画 prompt 工程师。
                        根据商品信息，生成一段用于 AI 图片生成的英文 prompt。
                        要求：用英文写，50-100 词，包含商品外观特征、背景、光线、风格，
                        商业摄影风格，适合电商展示。只输出 prompt 文本，不要其他内容。
                        """)
                .user(String.format(
                        "商品名称：%s\n商品描述：%s\n风格要求：%s",
                        request.productName(),
                        request.productDescription(),
                        request.style()))
                .call()
                .content();

        // 第二步：用 Prompt 生成图片
        ImageResponse imageResponse = imageModel.call(
                new ImagePrompt(
                        imagePrompt,
                        DashScopeImageOptions.builder()
                                .withModel("wanx2.1-t2i-plus")
                                .withN(1)
                                .withWidth(1024)
                                .withHeight(1024)
                                .build()
                )
        );

        return new GenerateResult(imagePrompt, imageResponse.getResult().getOutput().getUrl());
    }
}