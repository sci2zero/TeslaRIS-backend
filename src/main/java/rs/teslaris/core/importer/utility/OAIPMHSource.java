package rs.teslaris.core.importer.utility;

public enum OAIPMHSource {

    CRIS_UNS("https://cris.uns.ac.rs/OAIHandlerTeslaRIS");

    private final String value;

    OAIPMHSource(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;
    }
}
