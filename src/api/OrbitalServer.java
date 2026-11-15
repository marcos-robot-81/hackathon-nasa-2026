package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import db.LocalStore;
import engine.BodySystem;
import engine.RiskAlert;
import engine.RiskLevel;
import engine.SensorSnapshot;
import engine.TelemetrySimulator;
import i18n.I18n;
import i18n.Lang;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import user.AstronautProfile;

public final class OrbitalServer {

    private final int port;
    private final AstronautProfile profile;
    private final LocalStore store;
    private final TelemetrySimulator simulator;
    private final byte[] cachedLogoBytes;
    private HttpServer server;

    public OrbitalServer(int port, AstronautProfile profile, LocalStore store, TelemetrySimulator simulator) {
        this.port = port;
        this.profile = profile;
        this.store = store;
        this.simulator = simulator;
        this.cachedLogoBytes = loadLogoOnce();
    }

    private static byte[] loadLogoOnce() {
        try {
            Path p = Path.of("logo-OrbitalVitals.jpg");
            return Files.exists(p) ? Files.readAllBytes(p) : new byte[0];
        } catch (IOException e) {
            return new byte[0];
        }
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", this::handleIndex);
        server.createContext("/logo-OrbitalVitals.jpg", this::handleLogo);
        server.createContext("/api/telemetry", this::handleTelemetry);
        server.createContext("/api/control", this::handleControl);

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        Path htmlPath = Path.of("src", "web", "index.html");
        if (!Files.exists(htmlPath)) {
            sendText(exchange, 404, "text/plain; charset=utf-8", "Missing src/web/index.html");
            return;
        }
        byte[] bytes = Files.readAllBytes(htmlPath);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleLogo(HttpExchange exchange) throws IOException {
        if (cachedLogoBytes.length == 0) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
        exchange.getResponseHeaders().set("Cache-Control", "max-age=3600");
        exchange.sendResponseHeaders(200, cachedLogoBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(cachedLogoBytes);
        }
    }

    private void handleControl(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI());
        Integer days = params.containsKey("days") ? Integer.parseInt(params.get("days")) : null;
        Boolean eva = params.containsKey("eva") ? Boolean.parseBoolean(params.get("eva")) : null;
        Double gravity = params.containsKey("gravity") ? Double.parseDouble(params.get("gravity")) : null;
        Boolean storm = params.containsKey("storm") ? Boolean.parseBoolean(params.get("storm")) : null;

        simulator.updateControls(days, eva, gravity, storm);
        handleTelemetry(exchange);
    }

    private void handleTelemetry(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI());
        Lang lang = Lang.fromCode(params.get("lang"));
        SensorSnapshot s = store.latestSnapshot();

        Map<BodySystem, RiskLevel> sysStatus = new EnumMap<>(BodySystem.class);
        for (BodySystem sys : BodySystem.values()) {
            sysStatus.put(sys, RiskLevel.NOMINAL);
        }
        for (RiskAlert alert : store.activeAlerts()) {
            RiskLevel current = sysStatus.get(alert.system());
            if (alert.level().ordinal() > current.ordinal()) {
                sysStatus.put(alert.system(), alert.level());
            }
        }

        StringBuilder json = new StringBuilder(2560);
        json.append("{");
        json.append("\"lang\":\"").append(lang.code()).append("\",");
        json.append("\"appTitle\":\"").append(esc(I18n.t(lang, "app.title"))).append("\",");
        json.append("\"astronaut\":{")
            .append("\"id\":\"").append(esc(profile.id())).append("\",")
            .append("\"callSign\":\"").append(esc(profile.callSign())).append("\",")
            .append("\"missionDay\":").append(s.missionDays()).append(",")
            .append("\"locationLabel\":\"").append(esc(I18n.t(lang, s.outsideInEvaSuit() ? "loc.eva" : "loc.station"))).append("\"")
            .append("},");

        json.append(String.format(Locale.US,
            "\"sensors\":{" +
            "\"missionDays\":%d," +
            "\"outsideInEvaSuit\":%b," +
            "\"solarStormActive\":%b," +
            "\"gravityG\":%.2f," +
            "\"systolicBpMmHg\":%d," +
            "\"diastolicBpMmHg\":%d," +
            "\"aorticPeakVelocityCmS\":%.1f," +
            "\"spo2Pct\":%.1f," +
            "\"heartRateBpm\":%d," +
            "\"hrvMs\":%.1f," +
            "\"externalRadiationMgyDay\":%.2f," +
            "\"internalRadiationMgyDay\":%.2f," +
            "\"cumulativeRadiationMgy\":%.1f," +
            "\"shieldingProtectionPct\":%.0f," +
            "\"nlrImmuneRatio\":%.2f," +
            "\"crpInflammationMgL\":%.2f," +
            "\"ocularPressureMmHg\":%.1f," +
            "\"opticNerveShiftMm\":%.2f," +
            "\"opticNerveTiltDeg\":%.1f," +
            "\"boneLossRatePctMonth\":%.2f," +
            "\"cumulativeBoneLossPct\":%.2f," +
            "\"vertebralStrengthPct\":%.1f," +
            "\"cabinTempCelsius\":%.1f," +
            "\"cabinCo2MmHg\":%.1f" +
            "},",
            s.missionDays(), s.outsideInEvaSuit(), s.solarStormActive(), s.gravityG(),
            s.systolicBpMmHg(), s.diastolicBpMmHg(), s.aorticPeakVelocityCmS(), s.spo2Pct(),
            s.heartRateBpm(), s.hrvMs(),
            s.externalRadiationMgyDay(), s.internalAbsorbedRadiationMgyDay(), s.cumulativeRadiationMgy(), s.shieldingProtectionPct(),
            s.nlrImmuneRatio(), s.crpInflammationMgL(),
            s.ocularPressureMmHg(), s.opticNerveShiftMm(), s.opticNerveTiltDeg(),
            s.boneLossRatePctMonth(), s.cumulativeBoneLossPct(), s.vertebralStrengthPct(),
            s.cabinTempCelsius(), s.cabinCo2MmHg()
        ));

        json.append("\"systems\":{");
        BodySystem[] allSystems = BodySystem.values();
        for (int i = 0; i < allSystems.length; i++) {
            BodySystem sys = allSystems[i];
            RiskLevel lvl = sysStatus.get(sys);
            json.append("\"").append(sys.name()).append("\":{")
                .append("\"label\":\"").append(esc(sys.label(lang))).append("\",")
                .append("\"status\":\"").append(lvl.name()).append("\",")
                .append("\"statusLabel\":\"").append(esc(lvl.label(lang))).append("\",")
                .append("\"color\":\"").append(lvl.hexColor()).append("\"")
                .append("}");
            if (i < allSystems.length - 1) json.append(",");
        }
        json.append("},");

        json.append("\"alerts\":[");
        var alerts = store.activeAlerts();
        for (int i = 0; i < alerts.size(); i++) {
            RiskAlert a = alerts.get(i);
            json.append(String.format(Locale.US,
                "{\"system\":\"%s\",\"systemLabel\":\"%s\",\"level\":\"%s\",\"levelLabel\":\"%s\",\"color\":\"%s\"," +
                "\"title\":\"%s\",\"explain\":\"%s\",\"action\":\"%s\",\"osdrRef\":\"%s\",\"osdrTier\":\"%s\",\"osdrSourceFile\":\"%s\"," +
                "\"ewma\":%.2f,\"unit\":\"%s\",\"zScore\":%.2f}",
                a.system().name(), esc(a.system().label(lang)),
                a.level().name(), esc(a.level().label(lang)), a.level().hexColor(),
                esc(I18n.t(lang, a.titleKey())), esc(I18n.t(lang, a.explanationKey())), esc(I18n.t(lang, a.countermeasureKey())),
                esc(a.osdrAccession()), esc(a.osdrTier()), esc(a.osdrSourceFile()),
                a.ewmaValue(), esc(a.triggerMetric().unit()), a.zScore()
            ));
            if (i < alerts.size() - 1) json.append(",");
        }
        json.append("],");
        json.append("\"outboxCount\":").append(store.pendingEarthSync().size());
        json.append("}");

        sendText(exchange, 200, "application/json; charset=utf-8", json.toString());
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isBlank()) return map;
        for (String pair : q.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return map;
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
