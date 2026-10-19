package com.jichi.voiceshopping.voice;

import reactor.core.publisher.Flux;

public class SentenceAggregator {

    /**
     * 输入：逐 token 的文本片段
     * 输出：按 。！？， 聚合后的"可合成单元"
     */
    public static Flux<String> aggregate(Flux<String> tokens) {
        StringBuilder buf = new StringBuilder();
        return tokens.concatMap(token -> {
            buf.append(token);
            String s = buf.toString();
            int lastEnd = lastSentenceEnd(s);
            if (lastEnd > 0) {
                String ready = s.substring(0, lastEnd + 1);
                buf.delete(0, lastEnd + 1);
                return Flux.just(ready);
            }
            return Flux.<String>empty();
        }).concatWith(Flux.defer(() -> {
            // 收尾：模型最后一句可能没有标点（"…再聊聊"）
            if (buf.length() > 0) {
                String tail = buf.toString();
                buf.setLength(0);
                return Flux.just(tail);
            }
            return Flux.empty();
        }));
    }

    private static int lastSentenceEnd(String s) {
        int max = -1;
        for (char c : new char[]{'。', '！', '？', '，'}) {
            max = Math.max(max, s.lastIndexOf(c));
        }
        return max;
    }
}