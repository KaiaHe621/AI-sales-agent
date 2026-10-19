package com.jichi.prompt.entity;

import java.util.List;

public record ContractInfo(
    List<Party> parties,
    String contractNumber,
    String signDate,
    String effectiveDate,
    String expiryDate,
    ContractValue value,
    List<String> keyObligations,
    List<String> terminationConditions,
    String confidentialityClause,  // STRICT/GENERAL/NONE
    List<String> warnings
) {}