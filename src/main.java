import api.OrbitalServer;
import db.LocalStore;
import engine.AnomalyEngine;
import engine.RiskAlert;
import engine.SensorSnapshot;
import engine.TelemetrySimulator;
import i18n.I18n;
import i18n.Lang;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import user.AstronautProfile;

public class main {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        boolean cliOnce = false;
        Lang lang = Lang.EN;
        for (String arg : args) {
            if ("--once".equalsIgnoreCase(arg)) {
                cliOnce = true;
            } else {
                lang = Lang.fromCode(arg);
            }
        }

        AstronautProfile commander = AstronautProfile.defaultCommander();
        LocalStore store = new LocalStore();
        AnomalyEngine engine = new AnomalyEngine();
        TelemetrySimulator simulator = new TelemetrySimulator(commander, store, engine);

        printReport(lang, commander, store);

        if (cliOnce) {
            return;
        }

        int port = 8080;
        simulator.startVirtualThreadLoop();
        OrbitalServer server = new OrbitalServer(port, commander, store, simulator);
        server.start();

        Runtime rt = Runtime.getRuntime();
        double usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);
        double maxMb = rt.maxMemory() / (1024.0 * 1024.0);

        System.out.println("==================================================================");
        System.out.printf(Locale.US, " JVM Memory -> Used Heap: %.1f MB | Max Heap Limit: %.1f MB%n", usedMb, maxMb);
        System.out.println(" Web Dashboard running on JDK 25 Virtual Threads:");
        System.out.println(" -> Open in browser: http://localhost:" + port);
        System.out.println(" -> Press Ctrl+C to stop the server.");
        System.out.println("==================================================================");

        Thread.currentThread().join();
    }

    private static void printReport(Lang lang, AstronautProfile profile, LocalStore store) {
        SensorSnapshot s = store.latestSnapshot();
        Locale numLocale = (lang == Lang.PT) ? Locale.of("pt", "BR") : Locale.US;
        System.out.println("==================================================================");
        System.out.println(" " + I18n.t(lang, "app.title") + " [" + lang.code().toUpperCase() + "]");
        System.out.println(" Crew: " + profile.callSign() + " | " + I18n.t(lang, s.outsideInEvaSuit() ? "loc.eva" : "loc.station"));
        System.out.printf(numLocale, " Sensors -> BP: %d/%d mmHg | SpO2: %.1f%% | Gravity: %.2fG | Eye IOP: %.1f mmHg%n",
            s.systolicBpMmHg(), s.diastolicBpMmHg(), s.spo2Pct(), s.gravityG(), s.ocularPressureMmHg());
        System.out.println("==================================================================");

        for (RiskAlert alert : store.activeAlerts()) {
            System.out.printf(" [%s] %s (%s)%n",
                alert.level().label(lang),
                I18n.t(lang, alert.titleKey()),
                alert.system().label(lang)
            );
            System.out.printf(numLocale, "   EWMA: %.2f %s | Z-Score: %.2f | NASA OSDR Ref: %s%n",
                alert.ewmaValue(),
                alert.triggerMetric().unit(),
                alert.zScore(),
                alert.osdrAccession()
            );
            System.out.println("   Why:    " + I18n.t(lang, alert.explanationKey()));
            System.out.println("   Action: " + I18n.t(lang, alert.countermeasureKey()));
            System.out.println("------------------------------------------------------------------");
        }
    }
}
