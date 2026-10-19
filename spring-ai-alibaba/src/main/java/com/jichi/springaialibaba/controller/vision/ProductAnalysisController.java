package com.jichi.springaialibaba.controller.vision;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductAnalysisController {

    private final ChatClient chatClient;

    public ProductAnalysisController(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一个二手商品鉴定专家，擅长评估商品价值和状态。定价参考市场行情，客观准确。")
                .build();
    }

    record ProductAnalysis(
            @JsonPropertyDescription("商品类别，如：手机/笔记本/衣服")
            String category,

            @JsonPropertyDescription("品牌（如果能识别的话）")
            String brand,

            @JsonPropertyDescription("商品状态：全新/9成新/7-8成新/5-6成新/需维修")
            String condition,

            @JsonPropertyDescription("识别到的主要特征，最多5条")
            List<String> features,

            @JsonPropertyDescription("明显的瑕疵描述，没有则为空列表")
            List<String> defects,

            @JsonPropertyDescription("建议的二手定价区间，格式：最低价-最高价，单位元")
            String suggestedPriceRange,

            @JsonPropertyDescription("商品描述，适合用于二手交易平台的文案，100字以内")
            String description
    ) {
    }

    @PostMapping("/analyze")
    public ProductAnalysis analyzeProduct(
            @RequestParam("image") MultipartFile imageFile) throws Exception {

        MimeType mimeType = MimeType.valueOf(
                imageFile.getContentType() != null ? imageFile.getContentType() : "image/jpeg");

        Media media = Media.builder()
                .mimeType(mimeType)
                .data(imageFile.getResource())
                .build();

        UserMessage message = UserMessage.builder()
                .text("请分析这个二手商品的状况，并给出合理的定价建议")
                .media(media)
                .build();

        return chatClient.prompt()
                .messages(message)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max")
                        .withMultiModel(true)
                        .build())
                .call()
                .entity(ProductAnalysis.class);
    }
}