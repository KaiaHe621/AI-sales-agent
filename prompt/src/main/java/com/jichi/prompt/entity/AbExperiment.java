package com.jichi.prompt.entity;

public record AbExperiment(
    String experimentId,
    String promptKeyA,
    String versionA,
    String promptKeyB,
    String versionB,
    int trafficRatioA   // A 组流量占比 0-100，例如 50 表示各占一半
) {}