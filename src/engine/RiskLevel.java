package engine;

import i18n.I18n;
import i18n.Lang;

public enum RiskLevel {
    NOMINAL("status.nominal", "#14B8C4"),
    CAUTION("status.caution", "#F59E0B"),
    CRITICAL("status.critical", "#EF4444");

    private final String i18nKey;
    private final String hexColor;

    RiskLevel(String i18nKey, String hexColor) {
        this.i18nKey = i18nKey;
        this.hexColor = hexColor;
    }

    public String label(Lang lang) {
        return I18n.t(lang, i18nKey);
    }

    public String hexColor() {
        return hexColor;
    }
}
