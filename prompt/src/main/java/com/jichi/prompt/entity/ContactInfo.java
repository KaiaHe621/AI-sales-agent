package com.jichi.prompt.entity;

public record ContactInfo(
    String name,
    String phone,
    String email,
    String company,
    String position
) {}