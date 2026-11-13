package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.model.document.OtherEventType;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportEventRelation;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.converter.push.vivo.VivoEventMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoEventMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String URI = NS + "teslaris-event-3";

    private final VivoEventMapper mapper = new VivoEventMapper(
        new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"));


    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null,
            List.of("EN", "SR"), List.of("events"));
    }

    private static ExportEvent event(EventType eventType) {
        var event = new ExportEvent();
        event.setDatabaseId(3);
        event.setEventType(eventType);
        event.setName(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "Conference on Testing", 1),
            new ExportMultilingualContent("SR", "Konferencija o testiranju", 2),
            new ExportMultilingualContent("DE", "Testkonferenz", 3))));
        event.setNameAbbreviation(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "CT", 1))));
        event.setDescription(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "About testing.", 1))));
        event.setKeywords(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "testing\n quality \n\nsoftware", 1))));
        event.setDateFrom(LocalDate.of(2025, 5, 12));
        event.setDateTo(LocalDate.of(2025, 5, 14));
        event.setCountryCode("rs");
        return event;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
    }

    @Test
    public void shouldMapConference() {
        // When
        var model = mapper.toRdf(event(EventType.CONFERENCE), target());

        // Then
        var event = model.createResource(URI);
        assertTrue(has(model, URI, RDF.type.getURI(), "http://purl.org/ontology/bibo/Conference"));
        assertTrue(model.contains(event, RDFS.label,
            model.createLiteral("Conference on Testing", "en")));
        assertTrue(model.contains(event, RDFS.label,
            model.createLiteral("Konferencija o testiranju", "sr")));
        assertEquals(2, model.listObjectsOfProperty(event, RDFS.label).toList().size());
        assertTrue(model.contains(event, model.createProperty(VIVO + "abbreviation"),
            model.createLiteral("CT", "en")));
        assertTrue(model.contains(event, model.createProperty(VIVO + "description"),
            model.createLiteral("About testing.", "en")));
    }

    @Test
    public void shouldSplitKeywordsByLine() {
        // When
        var model = mapper.toRdf(event(EventType.CONFERENCE), target());

        // Then
        var keywords = model.listObjectsOfProperty(model.createResource(URI),
                model.createProperty(VIVO + "freetextKeyword")).toList().stream()
            .map(keyword -> keyword.asLiteral().getString()).sorted().toList();
        assertEquals(List.of("quality", "software", "testing"), keywords);
    }

    @Test
    public void shouldMapDatesCountryAndTeslarisLink() {
        // When
        var model = mapper.toRdf(event(EventType.CONFERENCE), target());

        // Then
        assertTrue(has(model, URI, VIVO + "dateTimeInterval", URI + "-interval"));
        assertTrue(has(model, URI + "-interval", VIVO + "start", URI + "-start"));
        assertTrue(has(model, URI + "-interval", VIVO + "end", URI + "-end"));
        assertTrue(model.contains(model.createResource(URI + "-end"),
            model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral("2025-05-14T00:00:00", XSDDatatype.XSDdateTime)));

        assertTrue(has(model, URI, "http://purl.obolibrary.org/obo/RO_0001025",
            "http://aims.fao.org/aos/geopolitical.owl#Serbia"));

        assertTrue(model.contains(model.createResource(URI + "-url-teslaris"),
            model.createProperty("http://www.w3.org/2006/vcard/ns#url"),
            model.createTypedLiteral("http://localhost:5173/sr/events/conference/3",
                XSDDatatype.XSDanyURI)));
    }

    @ParameterizedTest
    @CsvSource({
        "EXHIBITION,,http://vivoweb.org/ontology/core#Exhibit",
        "COURSE,,http://vivoweb.org/ontology/core#Course",
        "OTHER_EVENT,LECTURE,http://vivoweb.org/ontology/core#Presentation",
        "OTHER_EVENT,PLAY,http://purl.org/ontology/bibo/Performance",
        "OTHER_EVENT,TRIAL,http://purl.org/ontology/bibo/Hearing",
        "OTHER_EVENT,THESIS_DEFENCE,http://purl.org/NET/c4dm/event.owl#Event",
        "OTHER_EVENT,,http://purl.org/NET/c4dm/event.owl#Event"
    })
    public void shouldMapEventTypeToVivoClass(EventType eventType, OtherEventType otherEventType,
                                              String expectedClass) {
        // Given
        var event = event(eventType);
        event.setOtherEventType(otherEventType);

        // When
        var model = mapper.toRdf(event, target());

        // Then
        assertTrue(has(model, URI, RDF.type.getURI(), expectedClass));
        assertEquals(1,
            model.listObjectsOfProperty(model.createResource(URI), RDF.type).toList().size());
    }

    @Test
    public void shouldMapSerialConferenceAsConferenceSeries() {
        // Given
        var event = event(EventType.CONFERENCE);
        event.setSerialEvent(true);

        // When
        var model = mapper.toRdf(event, target());

        // Then
        assertTrue(has(model, URI, RDF.type.getURI(), VIVO + "ConferenceSeries"));
    }

    @Test
    public void shouldLinkPartOfAndSeriesRelationsInBothDirections() {
        // Given
        var event = event(EventType.CONFERENCE);
        event.getRelations().add(new ExportEventRelation(EventsRelationType.PART_OF, 10));
        event.getRelations()
            .add(new ExportEventRelation(EventsRelationType.BELONGS_TO_SERIES, 11));

        // When
        var model = mapper.toRdf(event, target());

        // Then
        var partOf = "http://purl.obolibrary.org/obo/BFO_0000050";
        var hasPart = "http://purl.obolibrary.org/obo/BFO_0000051";
        assertTrue(has(model, URI, partOf, NS + "teslaris-event-10"));
        assertTrue(has(model, NS + "teslaris-event-10", hasPart, URI));
        assertTrue(has(model, URI, partOf, NS + "teslaris-event-11"));
        assertTrue(has(model, NS + "teslaris-event-11", hasPart, URI));

        // The event writes only the links, not the type or label of the targets.
        assertFalse(model.contains(model.createResource(NS + "teslaris-event-11"), RDF.type));
        assertFalse(model.contains(model.createResource(NS + "teslaris-event-11"), RDFS.label));
    }

    @Test
    public void shouldNotSendCollocatedRelation() {
        // Given
        var event = event(EventType.CONFERENCE);
        event.getRelations()
            .add(new ExportEventRelation(EventsRelationType.COLLOCATED_WITH, 12));

        // When
        var model = mapper.toRdf(event, target());

        // Then
        assertFalse(model.containsResource(model.createResource(NS + "teslaris-event-12")));
    }

    @Test
    public void shouldMapOldSnapshotWithoutTypeOrDates() {
        // Given
        var event = event(null);
        event.setDateFrom(null);
        event.setDateTo(null);
        event.setCountryCode(null);

        // When
        var model = mapper.toRdf(event, target());

        // Then
        assertTrue(has(model, URI, RDF.type.getURI(), "http://purl.org/NET/c4dm/event.owl#Event"));
        assertFalse(model.contains(model.createResource(URI),
            model.createProperty(VIVO + "dateTimeInterval")));
        assertFalse(model.containsResource(model.createResource(URI + "-url-teslaris")));
        NTriplesSerializer.toLines(model);
    }
}
