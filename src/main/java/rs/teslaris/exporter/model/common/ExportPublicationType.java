package rs.teslaris.exporter.model.common;

public enum ExportPublicationType {
    JOURNAL_PUBLICATION("JOURNAL_PUBLICATION"),
    PROCEEDINGS("PROCEEDINGS"),
    PROCEEDINGS_PUBLICATION("PROCEEDINGS_PUBLICATION"),
    MONOGRAPH("MONOGRAPH"),
    PATENT("PATENT"),
    SOFTWARE("SOFTWARE"),
    DATASET("DATASET"),
    JOURNAL("JOURNAL"),
    MONOGRAPH_PUBLICATION("MONOGRAPH_PUBLICATION"),
    THESIS("THESIS"),
    MATERIAL_PRODUCT("MATERIAL_PRODUCT"),
    GENETIC_MATERIAL("GENETIC_MATERIAL"),
    BOOK_SERIES("BOOK_SERIES");

    private final String value;

    ExportPublicationType(String value) {
        this.value = value;
    }

    public static ExportPublicationType fromStringValue(String value) {
        for (var format : ExportPublicationType.values()) {
            if (format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
