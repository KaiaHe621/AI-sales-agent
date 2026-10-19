package com.jichi.springaialibaba.controller.voice;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/tts")
public class TextToSpeechController {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 基础合成：文字转语音，返回 mp3 字节流，同时保存到当前目录
     */
    @GetMapping(value = "/synthesize", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(@RequestParam String text) throws Exception {
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model("cosyvoice-v3-flash")
                .voice("longanyang")
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        synthesizer.getDuplexApi().close(1000, "bye");

        byte[] audioBytes = audio.array();

        // 保存到项目根目录（Spring Boot 运行时工作目录）
        Files.write(Path.of("speech.mp3"), audioBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(audioBytes);
    }

    /**
     * 自定义参数：指定音色和语速
     */
    @GetMapping(value = "/synthesize-custom", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeCustom(
            @RequestParam String text,
            @RequestParam(defaultValue = "longanyang") String voice,
            @RequestParam(defaultValue = "1.0") float speed) throws Exception {

        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model("cosyvoice-v3-flash")
                .voice(voice)
                .speechRate(speed)   // 语速：0.5（慢）- 2.0（快），1.0 正常
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        synthesizer.getDuplexApi().close(1000, "bye");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio.array());
    }
}