package rs.teslaris.core.model.commontypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BrandingTheme {
    LIGHT,
    DARK;

    @JsonCreator
    public static BrandingTheme fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "light" -> LIGHT;
            case "dark" -> DARK;
            default -> throw new IllegalArgumentException("invalidBrandingTheme");
        };
    }

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }
}
