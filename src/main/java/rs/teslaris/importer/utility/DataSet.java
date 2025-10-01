package rs.teslaris.importer.utility;

import java.util.HashMap;
import java.util.Map;
import rs.teslaris.core.model.oaipmh.event.Event;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.core.model.oaipmh.product.Product;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.importer.model.common.DocumentImport;

public enum DataSet {
    ORGANISATION_UNITS("openaire_cris_orgunits", OrgUnit.class),
    EVENTS("openaire_cris_events", Event.class),
    PERSONS("openaire_cris_persons", Person.class),
    PUBLICATIONS("openaire_cris_publications", Publication.class),
    PATENTS("openaire_cris_patents", Patent.class),
    PRODUCTS("openaire_cris_products", Product.class),


    // Used to differentiate between crisuns publication types
    CONFERENCE_PROCEEDINGS("crisuns_conference_proceedings", Publication.class),
    JOURNALS("crisuns_journals", Publication.class),
    RESEARCH_ARTICLES("crisuns_research_articles", Publication.class),
    CONFERENCE_PUBLICATIONS("crisuns_conference_publications", Publication.class),
    PHD_THESES("crisuns_phd_theses", Publication.class),
    MR_THESES("crisuns_mr_theses", Publication.class),
    MONOGRAPHS("crisuns_monographs", Publication.class),
    MONOGRAPH_PUBLICATIONS("crisuns_monograph_publications", Publication.class),


    // Used for common imports
    DOCUMENT_IMPORTS("common_imports", DocumentImport.class);


    private static final Map<String, Class<?>> classMap = new HashMap<>();

    static {
        for (DataSet dataSet : values()) {
            classMap.put(dataSet.value, dataSet.associatedClass);
        }
    }

    private final String value;
    private final Class<?> associatedClass;

    DataSet(String value, Class<?> associatedClass) {
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
