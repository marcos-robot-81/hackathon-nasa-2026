package user;

import engine.VitalMetric;
import java.util.Map;

public record AstronautProfile(
    String id,
    String callSign,
    String missionHabitat,
    int missionDay,
    int readinessScore,
    int streakDays,
    Map<VitalMetric, MetricBaseline> baselines
) {
    public static AstronautProfile defaultCommander() {
        return new AstronautProfile(
            "AST-01",
            "CDR. V. KOWALSKI",
            "Orbital Station / Centrifuge Hab",
            7,
            94,
            14,
            Map.of(
                VitalMetric.HEART_RATE_BPM, new MetricBaseline(64.0, 5.0),
                VitalMetric.HRV_RMSSD_MS, new MetricBaseline(58.0, 6.0),
                VitalMetric.SPO2_PCT, new MetricBaseline(98.2, 0.7),
                VitalMetric.SYSTOLIC_BP_MMHG, new MetricBaseline(118.0, 6.0),
                VitalMetric.DIASTOLIC_BP_MMHG, new MetricBaseline(76.0, 4.5),
                VitalMetric.OCULAR_IOP_MMHG, new MetricBaseline(14.5, 1.5),
                VitalMetric.BONE_LOSS_PCT_MONTH, new MetricBaseline(0.10, 0.08),
                VitalMetric.CUMULATIVE_BONE_LOSS_PCT, new MetricBaseline(0.20, 0.25),
                VitalMetric.RADIATION_INTERNAL_MGY_D, new MetricBaseline(0.22, 0.08),
                VitalMetric.CUMULATIVE_RADIATION_MGY, new MetricBaseline(5.0, 5.0)
            )
        );
    }
}
