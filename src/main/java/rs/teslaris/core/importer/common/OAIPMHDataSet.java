package rs.teslaris.core.importer.common;

public enum OAIPMHDataSet {

    ORGANISATION_UNITS("openaire_cris_orgunits"),
    EVENTS("openaire_cris_events"),
    PERSONS("openaire_cris_persons"),
    PUBLICATIONS("openaire_cris_publications"),
    PATENTS("openaire_cris_patents"),
    PRODUCTS("openaire_cris_products");

    private final String value;

    OAIPMHDataSet(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;

    }
}
