package rs.teslaris.importer.utility.oaipmh;

public enum OAIPMHMigrationSource {

    CRIS_UNS("https://cris.uns.ac.rs/OAIHandlerTeslaRISAll");

    private final String value;

    OAIPMHMigrationSource(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;
    }
}
