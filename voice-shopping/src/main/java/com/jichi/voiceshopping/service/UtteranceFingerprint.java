package com.jichi.voiceshopping.service;

import java.security.MessageDigest;

public class UtteranceFingerprint {

    /** 对用户原话做归一化后计算指纹 */
    public static String compute(String utterance) {
        String normalized = utterance == null ? "" : utterance
                .trim()
                .replaceAll("[，。！？,.!?\\s]", "")
                .toLowerCase();
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(normalized.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(normalized.hashCode());
        }
    }
}