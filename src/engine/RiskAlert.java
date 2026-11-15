package engine;

import java.time.Instant;

public record RiskAlert(
    String astronautId,
    BodySystem system,
    VitalMetric triggerMetric,
    RiskLevel level,
    double zScore,
    double ewmaValue,
    String titleKey,
    String explanationKey,
    String countermeasureKey,
    String osdrAccession,
    String osdrTier,
    String osdrSourceFile,
    Instant createdAt
) {}
