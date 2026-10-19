package com.jichi.prompt.entity;

import java.util.List;

public record ContactInfoWithConfidence(
    String name,
    Double nameConfidence,
    String phone,
    Double phoneConfidence,
    String email,
    Double emailConfidence,
    String company,
    Double companyConfidence,
    List<String> uncertainFields
) {}