package db;

import engine.BodySystem;
import engine.RiskAlert;
import engine.RiskLevel;
import engine.SensorSnapshot;
import engine.TelemetrySample;
import engine.VitalMetric;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LocalStore {

    public record OutboxPacket(
        String packetId,
        String astronautId,
        String payloadType,
        String summary,
        Instant queuedAt,
        boolean syncedToEarth
    ) {}

    // Bounded ring buffers per metric (zero array-copy overhead unlike CopyOnWriteArrayList)
    private static final int MAX_SAMPLES_PER_METRIC = 24;
    private final Map<VitalMetric, Deque<TelemetrySample>> metricBuffers = new EnumMap<>(VitalMetric.class);
    private final Map<BodySystem, RiskLevel> lastAlertedLevel = new EnumMap<>(BodySystem.class);
    private final Map<BodySystem, OsdrStudyRef> osdrCatalog = new EnumMap<>(BodySystem.class);
    private final List<OutboxPacket> earthSyncOutbox = new ArrayList<>(32);

    private volatile List<RiskAlert> activeAlerts = List.of();
    private volatile SensorSnapshot latestSnapshot;

    public LocalStore() {
        for (VitalMetric m : VitalMetric.values()) {
            metricBuffers.put(m, new ArrayDeque<>(MAX_SAMPLES_PER_METRIC));
        }
        for (BodySystem s : BodySystem.values()) {
            lastAlertedLevel.put(s, RiskLevel.NOMINAL);
        }
        seedCuratedOsdrStudies();
    }

    private void seedCuratedOsdrStudies() {
        osdrCatalog.put(BodySystem.CARDIOVASCULAR, new OsdrStudyRef(
            "OSD-435 (LSDS-22) / OSD-575 (LSDS-8)",
            "Tier 1 & Tier 2 — Orbital Human & Aortic Hemodynamics",
            "LSDS-22_Echocardiography_aorta_TRANSFORMED.csv",
            BodySystem.CARDIOVASCULAR,
            "Cardiovascular Hemodynamics, Aortic Blood Velocity & Multiplex Cytokines",
            "Tracks heart rate, peak aortic blood velocity (3-9 mo post-radiation) and HDCVD9 cardiovascular panel."
        ));
        osdrCatalog.put(BodySystem.MUSCULOSKELETAL, new OsdrStudyRef(
            "OSD-108 / OSD-310 (LSDS-30) / OSD-471 / OSD-942",
            "Tier 2 — Long-Duration Bone DEXA & Human 64d Bed-Rest",
            "s3://nasa-osdr/OSD-108/ & OSD-942 (*_TRANSFORMED.csv)",
            BodySystem.MUSCULOSKELETAL,
            "Bone Mineral Density (DEXA), Histomorphometry & Vertebral Flexural Strength",
            "Cumulative BMD loss (1.0-1.5%/mo in 0G) and vertebral mechanical strength degradation."
        ));
        osdrCatalog.put(BodySystem.NEURO_OCULAR, new OsdrStudyRef(
            "OSD-815, OSD-816, OSD-817, OSD-818",
            "Tier 2 — Human Ex Vivo SANS Translaminar Model",
            "LSDS-136 / LSDS-135 / LSDS-132 (Electroretinography & ICP:IOP)",
            BodySystem.NEURO_OCULAR,
            "Spaceflight-Associated Neuro-Ocular Syndrome (SANS) & Optic Nerve Tilt",
            "Cephalad fluid shift in 0G alters ICP:IOP translaminar gradient and optic nerve tilt."
        ));
        osdrCatalog.put(BodySystem.IMMUNE_RADIATION, new OsdrStudyRef(
            "RadLab API / OSD-569 (LSDS-7) / OSD-575 (LSDS-8)",
            "Tier 1 (Core) — Live Dosimetry & SpaceX Inspiration4 Hematology",
            "GET /radlab/api & LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv",
            BodySystem.IMMUNE_RADIATION,
            "Space Radiation Dosimetry (GCR/SPE), Neutrophil-Lymphocyte Ratio (NLR) & CRP",
            "External vs shielded radiation dose coupled with CBC neutrophil-lymphocyte ratio (NLR) and C-reactive protein (CRP)."
        ));
    }

    public synchronized void updateSnapshot(SensorSnapshot snapshot) {
        this.latestSnapshot = snapshot;
        Instant now = snapshot.timestamp();
        String id = snapshot.astronautId();
        appendTelemetry(new TelemetrySample(id, VitalMetric.HEART_RATE_BPM, snapshot.heartRateBpm(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.HRV_RMSSD_MS, snapshot.hrvMs(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.SPO2_PCT, snapshot.spo2Pct(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.SYSTOLIC_BP_MMHG, snapshot.systolicBpMmHg(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.OCULAR_IOP_MMHG, snapshot.ocularPressureMmHg(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.BONE_LOSS_PCT_MONTH, snapshot.boneLossRatePctMonth(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.CUMULATIVE_BONE_LOSS_PCT, snapshot.cumulativeBoneLossPct(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.RADIATION_INTERNAL_MGY_D, snapshot.internalAbsorbedRadiationMgyDay(), now));
        appendTelemetry(new TelemetrySample(id, VitalMetric.CUMULATIVE_RADIATION_MGY, snapshot.cumulativeRadiationMgy(), now));
    }

    public SensorSnapshot latestSnapshot() {
        return latestSnapshot;
    }

    public synchronized void appendTelemetry(TelemetrySample sample) {
        Deque<TelemetrySample> ring = metricBuffers.get(sample.metric());
        if (ring.size() >= MAX_SAMPLES_PER_METRIC) {
            ring.removeFirst();
        }
        ring.addLast(sample);
    }

    public synchronized void replaceAlerts(List<RiskAlert> newAlerts) {
        this.activeAlerts = List.copyOf(newAlerts);

        // Only enqueue an Earth Sync Outbox packet when a system transitions into a higher/new risk state
        Map<BodySystem, RiskLevel> currentLevels = new EnumMap<>(BodySystem.class);
        for (BodySystem s : BodySystem.values()) {
            currentLevels.put(s, RiskLevel.NOMINAL);
        }
        for (RiskAlert alert : newAlerts) {
            currentLevels.put(alert.system(), alert.level());
            RiskLevel prev = lastAlertedLevel.get(alert.system());
            if (alert.level() != RiskLevel.NOMINAL && alert.level() != prev && earthSyncOutbox.size() < 32) {
                earthSyncOutbox.add(new OutboxPacket(
                    "PKT-" + (earthSyncOutbox.size() + 1),
                    alert.astronautId(),
                    "SENSOR_ALERT_" + alert.level().name(),
                    alert.system().name() + " | " + alert.osdrAccession() + " | Z=" + String.format(Locale.US, "%.2f", alert.zScore()),
                    Instant.now(),
                    false
                ));
            }
        }
        for (BodySystem s : BodySystem.values()) {
            lastAlertedLevel.put(s, currentLevels.get(s));
        }
    }

    public synchronized List<TelemetrySample> telemetryByMetric(String astronautId, VitalMetric metric) {
        return List.copyOf(metricBuffers.get(metric));
    }

    public OsdrStudyRef studyForSystem(BodySystem system) {
        return osdrCatalog.get(system);
    }

    public List<RiskAlert> activeAlerts() {
        return activeAlerts;
    }

    public synchronized List<OutboxPacket> pendingEarthSync() {
        return Collections.unmodifiableList(new ArrayList<>(earthSyncOutbox));
    }
}
