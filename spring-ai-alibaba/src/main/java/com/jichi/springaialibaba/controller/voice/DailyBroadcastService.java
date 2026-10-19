package com.jichi.springaialibaba.controller.voice;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DailyBroadcastService {

    private final ChatClient chatClient;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public DailyBroadcastService(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel).build();
    }

    /**
     * 每天早上 8 点生成日报播报音频
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateDailyBroadcast() throws Exception {
        String script = chatClient.prompt()
                .system("你是一个播音员，生成简洁的早间播报文案，不要用 Markdown 格式")
                .user("今天是 " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")) +
                      "，请生成一段 30 秒的早间播报，包括问候语和今日关键提示")
                .call()
                .content();

        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model("cosyvoice-v3-flash")
                .voice("longxiaocheng")  // 男声播报更正式
                .speechRate(0.9f)        // 略慢，播报感
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(script);
        synthesizer.getDuplexApi().close(1000, "bye");

        // 保存到项目根目录，文件名带日期
        String filename = "broadcast-" + LocalDate.now() + ".mp3";
        try {
            Files.write(Path.of(filename), audio.array());
        } catch (IOException e) {
            throw new RuntimeException("保存播报文件失败", e);
        }
    }
}