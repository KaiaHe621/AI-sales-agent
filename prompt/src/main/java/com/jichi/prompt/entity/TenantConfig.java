package com.jichi.prompt.entity;

import java.util.List;

public record TenantConfig(
    String tenantId,
    String companyName,
    String businessType,
    List<String> capabilities,
    List<String> restrictions,
    String tone,
    String specialRequirements
) {}