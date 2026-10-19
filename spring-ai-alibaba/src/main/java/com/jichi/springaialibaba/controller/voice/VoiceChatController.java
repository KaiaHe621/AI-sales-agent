package com.jichi.springaialibaba.controller.voice;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/voice-chat")
public class VoiceChatController {

    private final ChatClient chatClient;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public VoiceChatController(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是一个语音助手，回答会被转成语音播放。
                        因此：
                        - 回答要口语化，避免用 Markdown 格式
                        - 不要输出代码块、列表符号（*、-）等
                        - 句子要自然流畅，适合朗读
                        - 回答控制在 100 字以内
                        """)
                .build();
    }

    /**
     * 文字提问，返回语音回答
     */
    @GetMapping(value = "/ask", produces = "audio/mpeg")
    public ResponseEntity<byte[]> askWithVoice(
            @RequestParam String question,
            @RequestParam(defaultValue = "longanyang") String voice) throws Exception {

        // 第一步：获取文字回答
        String textAnswer = chatClient.prompt()
                .user(question)
                .call()
                .content();

        // 第二步：把文字转成语音
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model("cosyvoice-v3-flash")
                .voice(voice)
                .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(textAnswer);
        synthesizer.getDuplexApi().close(1000, "bye");

        byte[] audioBytes = audio.array();
        Files.write(Path.of("voice-answer.mp3"), audioBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audioBytes);
    }
}