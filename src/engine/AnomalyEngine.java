package engine;

import db.LocalStore;
import db.OsdrStudyRef;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import user.AstronautProfile;
import user.MetricBaseline;

public final class AnomalyEngine {

    private static final double EWMA_ALPHA = 0.35;

    public double computeEwma(List<TelemetrySample> samples, double initialMean) {
        double ewma = initialMean;
        for (TelemetrySample s : samples) {
            ewma = (EWMA_ALPHA * s.value()) + ((1.0 - EWMA_ALPHA) * ewma);
        }
        return ewma;
    }

    public double recentWindowAverage(List<TelemetrySample> samples, int windowSize) {
        if (samples.isEmpty()) {
            return 0.0;
        }
        int w = Math.min(windowSize, samples.size());
        List<TelemetrySample> lastWindow = samples.subList(samples.size() - w, samples.size());
        return lastWindow.stream().mapToDouble(TelemetrySample::value).average().orElse(0.0);
    }

    public Optional<RiskAlert> evaluate(
        AstronautProfile profile,
        VitalMetric metric,
        List<TelemetrySample> history,
        LocalStore store
    ) {
        if (history.isEmpty()) {
            return Optional.empty();
        }
        MetricBaseline baseline = profile.baselines().get(metric);
        if (baseline == null) {
            return Optional.empty();
        }

        double ewma = computeEwma(history, baseline.mean());
        double zScore = baseline.zScore(ewma);
        OsdrStudyRef osdr = store.studyForSystem(metric.system());

        return switch (metric) {
            case HRV_RMSSD_MS -> {
                if (zScore <= -2.0) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CRITICAL, zScore, ewma,
                        "alert.hrv_drop.title", "alert.hrv_drop.explain", "alert.hrv_drop.action", osdr));
                } else if (zScore <= -1.2) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CAUTION, zScore, ewma,
                        "alert.hrv_drop.title", "alert.hrv_drop.explain", "alert.hrv_drop.action", osdr));
                }
                yield Optional.empty();
            }
            case RADIATION_INTERNAL_MGY_D -> {
                SensorSnapshot snap = store.latestSnapshot();
                double cumRad = (snap != null) ? snap.cumulativeRadiationMgy() : 0.0;
                if (ewma >= 1.10 || cumRad >= 45.0) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CRITICAL, zScore, ewma,
                        "alert.radiation_spike.title", "alert.radiation_spike.explain", "alert.radiation_spike.action", osdr));
                } else if (ewma >= 0.50 || cumRad >= 25.0) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CAUTION, zScore, ewma,
                        "alert.radiation_spike.title", "alert.radiation_spike.explain", "alert.radiation_spike.action", osdr));
                }
                yield Optional.empty();
            }
            case CUMULATIVE_BONE_LOSS_PCT -> {
                if (ewma >= 2.50) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CRITICAL, zScore, ewma,
                        "alert.deconditioning.title", "alert.deconditioning.explain", "alert.deconditioning.action", osdr));
                } else if (ewma >= 0.85) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CAUTION, zScore, ewma,
                        "alert.deconditioning.title", "alert.deconditioning.explain", "alert.deconditioning.action", osdr));
                }
                yield Optional.empty();
            }
            case OCULAR_IOP_MMHG -> {
                if (ewma >= 21.0) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CRITICAL, zScore, ewma,
                        "alert.ocular_sans.title", "alert.ocular_sans.explain", "alert.ocular_sans.action", osdr));
                } else if (ewma >= 18.0) {
                    yield Optional.of(buildAlert(profile, metric, RiskLevel.CAUTION, zScore, ewma,
                        "alert.ocular_sans.title", "alert.ocular_sans.explain", "alert.ocular_sans.action", osdr));
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    private RiskAlert buildAlert(
        AstronautProfile profile,
        VitalMetric metric,
        RiskLevel level,
        double zScore,
        double ewma,
        String titleKey,
        String explainKey,
        String actionKey,
        OsdrStudyRef osdr
    ) {
        String accession = (osdr != null) ? osdr.accession() : "OSD-REF";
        String tier = (osdr != null) ? osdr.tier() : "Tier 1";
        String sourceFile = (osdr != null) ? osdr.sourceFileOrApi() : "NASA OSDR";
        return new RiskAlert(
            profile.id(),
            metric.system(),
            metric,
            level,
            zScore,
            ewma,
            titleKey,
            explainKey,
            actionKey,
            accession,
            tier,
            sourceFile,
            Instant.now()
        );
    }
}
