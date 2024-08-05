package rs.teslaris.core.exporter.util;

public enum ExportDataFormat {

    OAI_CERIF_OPENAIRE("oai_cerif_openaire");

    private final String value;

    ExportDataFormat(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;
    }
}
