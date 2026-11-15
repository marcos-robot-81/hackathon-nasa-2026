package i18n;

public enum Lang {
    EN("en"),
    PT("pt");

    private final String code;

    Lang(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Lang fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return EN; // Default language is English for international judges
        }
        return raw.trim().toLowerCase().startsWith("pt") ? PT : EN;
    }
}
