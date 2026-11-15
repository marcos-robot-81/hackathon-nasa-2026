package engine;

public enum VitalMetric {
    HEART_RATE_BPM(BodySystem.CARDIOVASCULAR, "bpm"),
    HRV_RMSSD_MS(BodySystem.CARDIOVASCULAR, "ms"),
    SPO2_PCT(BodySystem.CARDIOVASCULAR, "%"),
    SYSTOLIC_BP_MMHG(BodySystem.CARDIOVASCULAR, "mmHg"),
    DIASTOLIC_BP_MMHG(BodySystem.CARDIOVASCULAR, "mmHg"),
    GRAVITY_CENTRIFUGAL_G(BodySystem.MUSCULOSKELETAL, "G"),
    BONE_LOSS_PCT_MONTH(BodySystem.MUSCULOSKELETAL, "%/mo"),
    CUMULATIVE_BONE_LOSS_PCT(BodySystem.MUSCULOSKELETAL, "% total"),
    OCULAR_IOP_MMHG(BodySystem.NEURO_OCULAR, "mmHg"),
    RADIATION_EXTERNAL_MGY_D(BodySystem.IMMUNE_RADIATION, "mGy/d"),
    RADIATION_INTERNAL_MGY_D(BodySystem.IMMUNE_RADIATION, "mGy/d"),
    CUMULATIVE_RADIATION_MGY(BodySystem.IMMUNE_RADIATION, "mGy total");

    private final BodySystem system;
    private final String unit;

    VitalMetric(BodySystem system, String unit) {
        this.system = system;
        this.unit = unit;
    }

    public BodySystem system() {
        return system;
    }

    public String unit() {
        return unit;
    }
}
