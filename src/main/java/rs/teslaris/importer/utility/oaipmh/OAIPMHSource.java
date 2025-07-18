package rs.teslaris.importer.utility.oaipmh;

public enum OAIPMHSource {

    CRIS_UNS("https://cris.uns.ac.rs/OAIHandlerTeslaRISAll");

    private final String value;

    OAIPMHSource(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;
    }
}
