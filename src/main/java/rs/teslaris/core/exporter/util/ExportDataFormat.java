package rs.teslaris.core.exporter.util;

import lombok.Getter;

public enum ExportDataFormat {
    OAI_CERIF_OPENAIRE("oai_cerif_openaire", "http://www.openaire.eu/schema/oai_cerif_openaire.xsd",
        "http://www.openaire.eu/namespace/oai_cerif_openaire"),
    DUBLIN_CORE("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd",
        "http://www.openarchives.org/OAI/2.0/oai_dc/");

    private final String value;

    @Getter
    private final String schema;

    @Getter
    private final String namespace;


    ExportDataFormat(String value, String schema, String namespace) {
        this.value = value;
        this.schema = schema;
        this.namespace = namespace;
    }

    public static ExportDataFormat fromStringValue(String value) {
        for (ExportDataFormat format : ExportDataFormat.values()) {
            if (format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    public String getStringValue() {
        return value;
    }
}

