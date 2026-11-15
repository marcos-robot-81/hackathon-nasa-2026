package engine;

import db.LocalStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import user.AstronautProfile;

public final class TelemetrySimulator {

    private final AstronautProfile profile;
    private final LocalStore store;
    private final AnomalyEngine engine;
    private final Random rng = new Random();

    private volatile int missionDays = 7;
    private volatile boolean outsideInEvaSuit = false;
    private volatile boolean solarStormActive = false;
    private volatile double gravityG = 0.0;

    public TelemetrySimulator(AstronautProfile profile, LocalStore store, AnomalyEngine engine) {
        this.profile = profile;
        this.store = store;
        this.engine = engine;
        for (int i = 0; i < 6; i++) {
            stepOnce();
        }
    }

    public void startVirtualThreadLoop() {
        Thread.ofVirtual().name("sensor-telemetry-loop").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    stepOnce();
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void updateControls(Integer days, Boolean evaSuit, Double newGravityG, Boolean solarStorm) {
        if (days != null) {
            this.missionDays = Math.max(1, Math.min(365, days));
        }
        if (evaSuit != null) {
            this.outsideInEvaSuit = evaSuit;
        }
        if (newGravityG != null) {
            this.gravityG = Math.max(0.0, Math.min(1.5, newGravityG));
        }
        if (solarStorm != null) {
            this.solarStormActive = solarStorm;
        }
        for (int i = 0; i < 6; i++) {
            stepOnce();
        }
    }

    public synchronized void stepOnce() {
        int days = this.missionDays;
        double g = this.gravityG;
        boolean eva = this.outsideInEvaSuit;
        boolean storm = this.solarStormActive;

        // 1. Radiation Dosimetry (RadLab API) + Inspiration4 Hematology/Inflammation (OSD-569 NLR & OSD-575 CRP)
        double baseExternalRad = storm ? (3.85 + rng.nextDouble() * 0.30) : (0.98 + rng.nextDouble() * 0.06);
        double shieldingFactor = eva ? 0.32 : 0.78;
        double internalRad = baseExternalRad * (1.0 - shieldingFactor);
        double cumulativeRad = internalRad * days;

        // NLR (Neutrophil-to-Lymphocyte Ratio - OSD-569 LSDS-7) & CRP (C-Reactive Protein - OSD-575 LSDS-8)
        double nlrRatio = 1.75 + (internalRad * 0.85) + (Math.min(1.0, days / 90.0) * 0.45) + (rng.nextDouble() - 0.5) * 0.10;
        double crpMgL = 0.80 + (internalRad * 1.40) + (storm ? 1.80 : 0.0) + (rng.nextDouble() - 0.5) * 0.12;

        // 2. Bone Mineral Density DEXA (OSD-108 / OSD-942) & Vertebral Flexural Strength (OSD-471 / LSDS-30)
        double unloadingFactor = Math.max(0.0, 1.0 - g);
        double boneLossMonth = 0.08 + (1.27 * unloadingFactor);
        double cumulativeBoneLoss = boneLossMonth * (days / 30.0);
        // Vertebral strength drops faster than areal BMD due to trabecular microarchitecture loss (OSD-471 / OSD-977)
        double vertebralStrengthPct = Math.max(65.0, 100.0 - (cumulativeBoneLoss * 1.65));

        // 3. SANS Ocular Model (OSD-815..818 / LSDS-136): IOP, Optic Disc Shift & Optic Nerve Tilt
        double chronicEyeFactor = Math.min(1.0, days / 45.0);
        double ocularIop = 14.2 + (4.2 * unloadingFactor) + (3.8 * unloadingFactor * chronicEyeFactor) + (rng.nextDouble() - 0.5) * 0.4;
        double opticNerveShift = 0.04 + (0.46 * unloadingFactor * Math.min(1.0, days / 60.0));
        double opticNerveTiltDeg = 1.2 + (6.8 * unloadingFactor * Math.min(1.0, days / 60.0));

        // 4. Cardiovascular & Aortic Echocardiography (OSD-435 / LSDS-22: Peak Aortic Blood Velocity cm/s)
        int sysBp = (int) Math.round(118 + (unloadingFactor * 10) + (eva ? 10 : 0) + (rng.nextDouble() - 0.5) * 3);
        int diaBp = (int) Math.round(76 + (unloadingFactor * 5) + (eva ? 5 : 0) + (rng.nextDouble() - 0.5) * 2);
        double aorticPeakVelCmS = 92.0 + (unloadingFactor * 16.0) + (eva ? 12.0 : 0.0) + (rng.nextDouble() - 0.5) * 2.5;
        double spo2 = (eva ? 96.5 : 98.3) + (rng.nextDouble() - 0.5) * 0.4;
        int hr = (int) Math.round(64 + (eva ? 22 : 0) + (storm ? 8 : 0) + (rng.nextDouble() - 0.5) * 3);
        double hrv = 58.0 - (eva ? 14.0 : 0.0) - (storm ? 8.0 : 0.0) - (unloadingFactor * 4.0 * chronicEyeFactor) + (rng.nextDouble() - 0.5) * 2.0;

        SensorSnapshot snap = new SensorSnapshot(
            profile.id(),
            days,
            eva,
            storm,
            g,
            sysBp,
            diaBp,
            aorticPeakVelCmS,
            spo2,
            hr,
            hrv,
            baseExternalRad,
            internalRad,
            cumulativeRad,
            shieldingFactor * 100.0,
            nlrRatio,
            crpMgL,
            ocularIop,
            opticNerveShift,
            opticNerveTiltDeg,
            boneLossMonth,
            cumulativeBoneLoss,
            vertebralStrengthPct,
            eva ? 21.2 : 23.8,
            eva ? 3.1 : 2.4,
            Instant.now()
        );

        store.updateSnapshot(snap);

        List<RiskAlert> alerts = new ArrayList<>();
        for (VitalMetric m : List.of(
            VitalMetric.RADIATION_INTERNAL_MGY_D,
            VitalMetric.OCULAR_IOP_MMHG,
            VitalMetric.CUMULATIVE_BONE_LOSS_PCT,
            VitalMetric.HRV_RMSSD_MS
        )) {
            engine.evaluate(profile, m, store.telemetryByMetric(profile.id(), m), store)
                .ifPresent(alerts::add);
        }
        store.replaceAlerts(alerts);
    }
}
