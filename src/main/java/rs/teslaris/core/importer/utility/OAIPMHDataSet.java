package rs.teslaris.core.importer.utility;

import java.util.HashMap;
import java.util.Map;
import rs.teslaris.core.importer.model.oaipmh.event.Event;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.oaipmh.patent.Patent;
import rs.teslaris.core.importer.model.oaipmh.product.Product;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;
import rs.teslaris.core.model.person.Person;

public enum OAIPMHDataSet {
    ORGANISATION_UNITS("openaire_cris_orgunits", OrgUnit.class),
    EVENTS("openaire_cris_events", Event.class),
    PERSONS("openaire_cris_persons", Person.class),
    PUBLICATIONS("openaire_cris_publications", Publication.class),
    PATENTS("openaire_cris_patents", Patent.class),
    PRODUCTS("openaire_cris_products", Product.class),


    // Used to differentiate between crisuns publication types
    CONFERENCE_PROCEEDINGS("crisuns_conference-proceedings", Publication.class),
    JOURNALS("crisuns_journals", Publication.class),
    RESEARCH_ARTICLES("crisuns_research_articles", Publication.class),
    CONFERENCE_PUBLICATIONS("crisuns_conference_publications", Publication.class);

    private static final Map<String, Class<?>> classMap = new HashMap<>();

    static {
        for (OAIPMHDataSet dataSet : values()) {
            classMap.put(dataSet.value, dataSet.associatedClass);
        }
    }

    private final String value;
    private final Class<?> associatedClass;

    OAIPMHDataSet(String value, Class<?> associatedClass) {
        this.value = value;
        this.associatedClass = associatedClass;
    }

    public static Class<?> getClassForValue(String value) {
        return classMap.get(value);
    }

    public String getStringValue() {
        return value;
    }
}
