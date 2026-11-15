package engine;

import java.time.Instant;

public record SensorSnapshot(
    String astronautId,
    int missionDays,
    boolean outsideInEvaSuit,
    boolean solarStormActive,
    double gravityG,
    int systolicBpMmHg,
    int diastolicBpMmHg,
    double aorticPeakVelocityCmS,
    double spo2Pct,
    int heartRateBpm,
    double hrvMs,
    double externalRadiationMgyDay,
    double internalAbsorbedRadiationMgyDay,
    double cumulativeRadiationMgy,
    double shieldingProtectionPct,
    double nlrImmuneRatio,
    double crpInflammationMgL,
    double ocularPressureMmHg,
    double opticNerveShiftMm,
    double opticNerveTiltDeg,
    double boneLossRatePctMonth,
    double cumulativeBoneLossPct,
    double vertebralStrengthPct,
    double cabinTempCelsius,
    double cabinCo2MmHg,
    Instant timestamp
) {}
