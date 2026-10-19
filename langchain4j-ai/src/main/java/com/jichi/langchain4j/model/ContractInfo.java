package com.jichi.langchain4j.model;

import java.util.List;

public record ContractInfo(
    String contractNumber,
    List<ContractParty> parties,
    String signDate,
    Double amount,
    String currency,
    List<String> keyObligations,
    List<String> warnings
) {}