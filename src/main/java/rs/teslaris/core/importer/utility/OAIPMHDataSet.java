package rs.teslaris.core.importer.utility;

public enum OAIPMHDataSet {

    ORGANISATION_UNITS("openaire_cris_orgunits"),
    EVENTS("openaire_cris_events"),
    PERSONS("openaire_cris_persons"),
    PUBLICATIONS("openaire_cris_publications"),
    PATENTS("openaire_cris_patents"),
    PRODUCTS("openaire_cris_products"),


    // Used to differentiate between crisuns publication types
    CONFERENCE_PROCEEDINGS("crisuns_conference-proceedings"),
    JOURNALS("crisuns_journals"),
    RESEARCH_ARTICLES("crisuns_research_articles"),
    CONFERENCE_PUBLICATIONS("crisuns_conference_publications");

    private final String value;

    OAIPMHDataSet(String value) {
        this.value = value;
    }

    public String getStringValue() {
        return value;
    }
}
