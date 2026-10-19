package com.jichi.voiceshopping.voice;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.common.ResultCallback;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Slf4j
@Component
public class TtsService {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${voice-shopping.voice.tts-model}")
    private String model;

    @Value("${voice-shopping.voice.tts-voice}")
    private String voice;

    /**
     * 一次流式合成。
     * @param textStream LLM 生成的文字流
     * @return 音频帧流（PCM 16k 16bit）
     */
    public Flowable<ByteBuffer> synthesize(Flowable<String> textStream) {
        // SpeechSynthesisAudioFormat 已经把"编码 + 采样率 + 位深"封装在一个枚举里，
        // 不需要再单独 .sampleRate(...)，避免两处配置不一致
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model(model)
                .voice(voice)
                .format(SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT)
                .build();

        return Flowable.create(emitter -> {
            // ResultCallback 必须传，否则 SDK 在 startStream 抛 InputRequiredException
            ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<>() {
                @Override
                public void onEvent(SpeechSynthesisResult result) {
                    ByteBuffer frame = result.getAudioFrame();
                    if (frame != null && frame.remaining() > 0 && !emitter.isCancelled()) {
                        emitter.onNext(frame);
                    }
                }

                @Override
                public void onComplete() {
                    if (!emitter.isCancelled()) emitter.onComplete();
                }

                @Override
                public void onError(Exception e) {
                    log.error("TTS 合成失败", e);
                    if (!emitter.isCancelled()) emitter.onError(e);
                }
            };

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);

            // 文字流来一段就 streamingCall 一段，结束时调 streamingComplete 触发尾包
            textStream.subscribe(
                    synthesizer::streamingCall,
                    emitter::onError,
                    synthesizer::streamingComplete
            );
        }, BackpressureStrategy.BUFFER);
    }
}