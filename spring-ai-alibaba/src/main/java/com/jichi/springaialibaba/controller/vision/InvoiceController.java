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

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    private final ChatClient chatClient;

    public InvoiceController(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是一个发票信息提取助手。
                        精确提取发票上的信息，不要猜测，看不清楚的字段返回 null。
                        金额统一用数字表示，不要带"元"或"¥"符号。
                        """)
                .build();
    }

    record InvoiceInfo(
            @JsonPropertyDescription("发票号码")
            String invoiceNumber,

            @JsonPropertyDescription("开票日期，格式 yyyy-MM-dd")
            String invoiceDate,

            @JsonPropertyDescription("销售方名称（卖家）")
            String sellerName,

            @JsonPropertyDescription("销售方税号")
            String sellerTaxId,

            @JsonPropertyDescription("购买方名称（买家）")
            String buyerName,

            @JsonPropertyDescription("购买方税号")
            String buyerTaxId,

            @JsonPropertyDescription("不含税金额，纯数字")
            Double amountExcludingTax,

            @JsonPropertyDescription("税额，纯数字")
            Double taxAmount,

            @JsonPropertyDescription("价税合计（含税总金额），纯数字")
            Double totalAmount,

            @JsonPropertyDescription("货物或服务名称")
            String items
    ) {
    }

    @PostMapping("/extract")
    public InvoiceInfo extractInvoice(
            @RequestParam("file") MultipartFile file) throws Exception {

        MimeType mimeType = MimeType.valueOf(
                file.getContentType() != null ? file.getContentType() : "image/jpeg");

        Media media = Media.builder()
                .mimeType(mimeType)
                .data(file.getResource())
                .build();

        UserMessage message = UserMessage.builder()
                .text("请提取这张发票上的所有信息")
                .media(media)
                .build();

        return chatClient.prompt()
                .messages(message)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max")
                        .withMultiModel(true)
                        .build())
                .call()
                .entity(InvoiceInfo.class);
    }
}