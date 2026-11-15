package i18n;

import java.util.Map;

public final class I18n {

    private static final Map<String, String> EN_TEXT = Map.ofEntries(
        Map.entry("app.title", "OrbitalVitals - Autonomous Astronaut Sensor Telemetry"),
        Map.entry("status.nominal", "NOMINAL"),
        Map.entry("status.caution", "CAUTION"),
        Map.entry("status.critical", "CRITICAL"),
        Map.entry("loc.station", "INSIDE STATION (Shielded Habitat)"),
        Map.entry("loc.eva", "OUTSIDE STATION (EVA Spacesuit)"),
        Map.entry("sys.cardiovascular", "Cardiovascular & Blood Oxygen"),
        Map.entry("sys.musculoskeletal", "Bone Density & Muscle Load"),
        Map.entry("sys.neuro_ocular", "Eyes & Ocular Pressure (SANS)"),
        Map.entry("sys.immune_radiation", "Radiation Exposure (Suit vs Station)"),
        Map.entry("alert.hrv_drop.title", "Autonomic Stress / HRV Drift Detected"),
        Map.entry("alert.hrv_drop.explain", "Heart Rate Variability dropped below pre-flight baseline over the sliding window."),
        Map.entry("alert.hrv_drop.action", "Reduce physical exertion and verify suit cooling & hydration line."),
        Map.entry("alert.radiation_spike.title", "High Radiation Dose Inside Suit / Module"),
        Map.entry("alert.radiation_spike.explain", "Absorbed radiation rate exceeds safe threshold due to external flux or reduced EVA suit shielding."),
        Map.entry("alert.radiation_spike.action", "Abort EVA or move to heavily shielded station core immediately (OSD-168 / RadLab)."),
        Map.entry("alert.deconditioning.title", "Accelerated Bone Loss in Low Gravity"),
        Map.entry("alert.deconditioning.explain", "Near-zero gravity/centrifugal force is accelerating femoral and spinal bone mineral loss."),
        Map.entry("alert.deconditioning.action", "Increase centrifugal rotation gravity or execute ARED resistive loading (OSD-108 / OSD-942)."),
        Map.entry("alert.ocular_sans.title", "Cephalad Fluid Shift & Ocular Pressure (SANS)"),
        Map.entry("alert.ocular_sans.explain", "Lack of gravity pulls blood toward the head, increasing intraocular & intracranial pressure."),
        Map.entry("alert.ocular_sans.action", "Activate artificial centrifugal gravity or lower-body negative pressure cuff (OSD-816).")
    );

    private static final Map<String, String> PT_TEXT = Map.ofEntries(
        Map.entry("app.title", "OrbitalVitals - Telemetria Autônoma de Sensores Espaciais"),
        Map.entry("status.nominal", "NOMINAL"),
        Map.entry("status.caution", "ATENÇÃO"),
        Map.entry("status.critical", "CRÍTICO"),
        Map.entry("loc.station", "DENTRO DA ESTAÇÃO (Módulo Blindado)"),
        Map.entry("loc.eva", "FORA DA ESTAÇÃO (Traje Espacial EVA)"),
        Map.entry("sys.cardiovascular", "Cardiovascular & Oxigenação"),
        Map.entry("sys.musculoskeletal", "Densidade Óssea & Carga Muscular"),
        Map.entry("sys.neuro_ocular", "Olhos & Pressão Ocular (SANS)"),
        Map.entry("sys.immune_radiation", "Exposição à Radiação (Traje vs Estação)"),
        Map.entry("alert.hrv_drop.title", "Estresse Autonômico / Queda de VFC Detectada"),
        Map.entry("alert.hrv_drop.explain", "A Variabilidade da Frequência Cardíaca caiu abaixo do baseline pré-voo na janela móvel."),
        Map.entry("alert.hrv_drop.action", "Reduzir esforço físico e verificar refrigeração e hidratação do traje."),
        Map.entry("alert.radiation_spike.title", "Dose Elevada de Radiação no Traje / Módulo"),
        Map.entry("alert.radiation_spike.explain", "Taxa de radiação absorvida acima do limite seguro devido ao fluxo externo ou menor blindagem do traje EVA."),
        Map.entry("alert.radiation_spike.action", "Abortar EVA ou deslocar para o núcleo blindado da estação imediatamente (OSD-168 / RadLab)."),
        Map.entry("alert.deconditioning.title", "Perda Óssea Acelerada por Baixa Gravidade"),
        Map.entry("alert.deconditioning.explain", "Gravidade/força centrífuga próxima de zero está acelerando a desmineralização óssea."),
        Map.entry("alert.deconditioning.action", "Aumentar gravidade na centrífuga ou executar carga resistida no ARED (OSD-108 / OSD-942)."),
        Map.entry("alert.ocular_sans.title", "Subida de Fluidos e Pressão nos Olhos (SANS)"),
        Map.entry("alert.ocular_sans.explain", "A falta de gravidade desloca o sangue para a cabeça, elevando a pressão intraocular e intracraniana."),
        Map.entry("alert.ocular_sans.action", "Ativar força centrífuga artificial ou manguito de pressão negativa nos membros inferiores (OSD-816).")
    );

    private I18n() {}

    public static String t(Lang lang, String key) {
        Map<String, String> bundle = (lang == Lang.PT) ? PT_TEXT : EN_TEXT;
        return bundle.getOrDefault(key, EN_TEXT.getOrDefault(key, key));
    }
}
