package engine;

import i18n.I18n;
import i18n.Lang;

public enum BodySystem {
    CARDIOVASCULAR("sys.cardiovascular"),
    MUSCULOSKELETAL("sys.musculoskeletal"),
    NEURO_OCULAR("sys.neuro_ocular"),
    IMMUNE_RADIATION("sys.immune_radiation");

    private final String i18nKey;

    BodySystem(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String label(Lang lang) {
        return I18n.t(lang, i18nKey);
    }
}
