package engine;

import java.time.Instant;

public record TelemetrySample(
    String astronautId,
    VitalMetric metric,
    double value,
    Instant timestamp
) {}
