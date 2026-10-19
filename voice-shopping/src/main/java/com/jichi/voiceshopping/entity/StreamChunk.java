package com.jichi.voiceshopping.entity;

import java.nio.ByteBuffer;

public record StreamChunk(Type type, String text, ByteBuffer audio, Object products) {
    public enum Type {TEXT, AUDIO, PRODUCTS}

    public static StreamChunk text(String t) {
        return new StreamChunk(Type.TEXT, t, null, null);
    }

    public static StreamChunk audio(ByteBuffer a) {
        return new StreamChunk(Type.AUDIO, null, a, null);
    }

    public static StreamChunk products(Object p) {
        return new StreamChunk(Type.PRODUCTS, null, null, p);
    }
}