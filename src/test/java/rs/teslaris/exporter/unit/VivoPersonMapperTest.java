package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import rs.teslaris.core.model.person.DegreeType;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.exporter.model.common.ExportEducation;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportEntityIdentifier;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.converter.push.vivo.VivoPersonMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoPersonMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    private final VivoPersonMapper mapper = new VivoPersonMapper(
        new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"), messageSource());


    static StaticMessageSource messageSource() {
        var messageSource = new StaticMessageSource();
        messageSource.addMessage("degreeType.PHD", Locale.ENGLISH, "Doctorate");
        messageSource.addMessage("degreeTypeAbbreviation.PHD", Locale.ENGLISH, "PhD");
        messageSource.addMessage("involvementType.EMPLOYED_AT", Locale.ENGLISH, "Employment");
        messageSource.addMessage("involvementType.EMPLOYED_AT", Locale.forLanguageTag("sr"),
            "Zaposlenje");
        return messageSource;
    }

    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null, null,
            List.of("persons"), null);
    }

    private static ExportOrganisationUnit organisationUnit(Integer id) {
        var organisationUnit = new ExportOrganisationUnit();
        organisationUnit.setDatabaseId(id);
        return organisationUnit;
    }

    private static ExportPerson person() {
        var person = new ExportPerson();
        person.setDatabaseId(7);
        person.setName(new ExportPersonName("Ivan", "M.", "Mrsulja"));
        person.setOrcid("0000-0002-1825-0097");
        person.setScopusAuthorId("57200000000");
        person.setElectronicAddresses(new ArrayList<>(List.of("ivan@uns.ac.rs")));
        person.setIdentifiers(new ArrayList<>(List.of(
            new ExportEntityIdentifier("ECRIS", "12345", "https://ecris.example/{ID}"),
            new ExportEntityIdentifier("NO_URL", "999", null))));
        person.setEmployments(new ArrayList<>(List.of(
            new ExportEmployment(organisationUnit(2), LocalDate.of(2020, 3, 1), null,
                "Teaching Assistant", 41, InvolvementType.EMPLOYED_AT, new ArrayList<>()),
            new ExportEmployment(organisationUnit(3), null, null, null, 42, null,
                new ArrayList<>()),
            new ExportEmployment(organisationUnit(4), LocalDate.of(2019, 1, 1), null, "Old",
                null, InvolvementType.EMPLOYED_AT, new ArrayList<>()))));
        return person;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
    }

    private static ExportEmployment externalEmployment(Integer involvementId,
                                                       String... englishNames) {
        var names = new ArrayList<ExportMultilingualContent>();
        for (var name : englishNames) {
            names.add(new ExportMultilingualContent("EN", name, 1));
        }
        return new ExportEmployment(null, null, null, "Visiting Professor", involvementId,
            InvolvementType.EMPLOYED_AT, names);
    }

    @Test
    public void shouldMapPersonNameVcardAndIdentifiers() {
        // When
        var model = mapper.toRdf(person(), target());

        // Then
        var uri = NS + "teslaris-person-7";
        var person = model.createResource(uri);
        var name = model.createResource(uri + "-vcard-name");
        var email = model.createResource(uri + "-vcard-email-1");

        assertTrue(has(model, uri, RDF.type.getURI(), "http://xmlns.com/foaf/0.1/Person"));
        assertTrue(model.contains(person, RDFS.label, "Mrsulja, Ivan M."));
        assertTrue(has(model, uri, "http://purl.obolibrary.org/obo/ARG_2000028",
            uri + "-vcard"));
        assertTrue(has(model, uri + "-vcard", RDF.type.getURI(), VCARD + "Individual"));
        assertTrue(model.contains(name, model.createProperty(VCARD + "givenName"), "Ivan"));
        assertTrue(model.contains(name, model.createProperty(VIVO + "middleName"), "M."));
        assertTrue(model.contains(name, model.createProperty(VCARD + "familyName"), "Mrsulja"));
        assertTrue(has(model, uri + "-vcard-email-1", RDF.type.getURI(), VCARD + "Work"));
        assertTrue(model.contains(email, model.createProperty(VCARD + "email"), "ivan@uns.ac.rs"));
        assertTrue(has(model, uri, VIVO + "orcidId", "https://orcid.org/0000-0002-1825-0097"));
        assertTrue(model.contains(person, model.createProperty(VIVO + "scopusId"),
            "57200000000"));
        assertTrue(model.contains(model.createResource(uri + "-url-teslaris"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("http://localhost:5173/sr/persons/7",
                XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(model.createResource(uri + "-url-ecris"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://ecris.example/12345", XSDDatatype.XSDanyURI)));
        assertFalse(model.containsResource(model.createResource(uri + "-url-no-url")));
    }

    @Test
    public void shouldMapPositionsWithRoleAndStartDate() {
        // When
        var model = mapper.toRdf(person(), target());

        // Then
        var person = NS + "teslaris-person-7";
        var position = NS + "teslaris-position-41";
        var organisationUnit = NS + "teslaris-ou-2";

        assertTrue(has(model, position, RDF.type.getURI(), VIVO + "Position"));
        assertTrue(model.contains(model.createResource(position), RDFS.label,
            "Teaching Assistant"));
        assertTrue(has(model, position, VIVO + "relates", person));
        assertTrue(has(model, position, VIVO + "relates", organisationUnit));
        assertTrue(has(model, person, VIVO + "relatedBy", position));
        assertTrue(has(model, organisationUnit, VIVO + "relatedBy", position));
        assertTrue(has(model, position + "-interval", VIVO + "start", position + "-start"));
        assertTrue(model.contains(model.createResource(position + "-start"),
            model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral("2020-03-01T12:00:00Z", XSDDatatype.XSDdateTime)));
        assertFalse(model.contains(model.createResource(position + "-interval"),
            model.createProperty(VIVO + "end")));
    }

    @Test
    public void shouldMapPositionWithoutRoleOrDatesAndSkipPositionWithoutInvolvementId() {
        // When
        var model = mapper.toRdf(person(), target());

        // Then
        var position = model.createResource(NS + "teslaris-position-42");
        assertTrue(model.contains(position, RDF.type));
        assertFalse(model.contains(position, RDFS.label));
        assertFalse(model.contains(position, model.createProperty(VIVO + "dateTimeInterval")));
        assertFalse(model.containsResource(model.createResource(NS + "teslaris-ou-4")));
    }

    @Test
    public void shouldNotSendSex() {
        // Given
        var person = person();
        person.setSex(Sex.FEMALE);

        // When
        var lines = NTriplesSerializer.toLines(mapper.toRdf(person, target()));

        // Then
        assertEquals(0, lines.stream().filter(line -> line.contains("FEMALE") ||
            line.toLowerCase().contains("gender")).count());
    }

    @Test
    public void shouldLinkExternalInstitutionAsSharedNode() {
        // Given
        var person = person();
        person.getEmployments().add(externalEmployment(43, "University of Belgrade"));

        // When
        var mapping = mapper.map(person, target());

        // Then
        assertEquals(1, mapping.sharedNodes().size());
        var node = mapping.sharedNodes().getFirst();
        assertTrue(node.uri().startsWith(NS + "teslaris-external-org-"));

        var nodeModel = node.model();
        assertTrue(has(nodeModel, node.uri(), RDF.type.getURI(),
            "http://xmlns.com/foaf/0.1/Organization"));
        assertTrue(nodeModel.contains(nodeModel.createResource(node.uri()), RDFS.label,
            nodeModel.createLiteral("University of Belgrade", "en")));

        // The person owns only the links, not the type and label of the institution.
        var model = mapping.model();
        var position = NS + "teslaris-position-43";
        assertTrue(has(model, position, VIVO + "relates", node.uri()));
        assertTrue(has(model, node.uri(), VIVO + "relatedBy", position));
        assertFalse(model.contains(model.createResource(node.uri()), RDFS.label));
        assertFalse(model.contains(model.createResource(node.uri()), RDF.type));
    }

    @Test
    public void shouldUseSameUriForSameExternalNameWithOtherCaseAndSpaces() {
        // Given
        var first = person();
        first.getEmployments().add(externalEmployment(43, "University of Belgrade"));
        var second = person();
        second.setDatabaseId(8);
        second.getEmployments().add(externalEmployment(44, "  university   OF belgrade "));
        var third = person();
        third.setDatabaseId(9);
        third.getEmployments().add(externalEmployment(45, "University of Novi Sad"));

        // When
        var firstUri = mapper.map(first, target()).sharedNodes().getFirst().uri();
        var secondUri = mapper.map(second, target()).sharedNodes().getFirst().uri();
        var thirdUri = mapper.map(third, target()).sharedNodes().getFirst().uri();

        // Then
        assertEquals(firstUri, secondUri);
        assertNotEquals(firstUri, thirdUri);
    }

    @Test
    public void shouldSkipExternalEmploymentWithoutName() {
        // Given
        var person = person();
        person.getEmployments().add(externalEmployment(43));

        // When
        var mapping = mapper.map(person, target());

        // Then
        assertTrue(mapping.sharedNodes().isEmpty());
        assertFalse(mapping.model().containsResource(
            mapping.model().createResource(NS + "teslaris-position-43")));
    }

    @Test
    public void shouldUseTranslatedInvolvementTypeWhenRoleIsMissing() {
        // Given
        var person = person();
        person.getEmployments().add(new ExportEmployment(organisationUnit(5), null, null, null,
            46, InvolvementType.EMPLOYED_AT, new ArrayList<>()));
        person.getEmployments().add(new ExportEmployment(organisationUnit(6), null, null, " ",
            47, InvolvementType.CANDIDATE, new ArrayList<>()));
        var target = new PushTarget("vivo-local", "vivo", true,
            "http://vivo.example/api/sparqlUpdate", "http://graph.example", NS,
            new Auth(AuthType.NONE, null, null), 100, null, List.of("EN", "SR", "DE"),
            List.of("persons"), null);

        // When
        var model = mapper.toRdf(person, target);

        // Then
        var position = model.createResource(NS + "teslaris-position-46");
        assertTrue(model.contains(position, RDFS.label, model.createLiteral("Employment", "en")));
        assertTrue(model.contains(position, RDFS.label, model.createLiteral("Zaposlenje", "sr")));
        assertEquals(2, model.listObjectsOfProperty(position, RDFS.label).toList().size());

        // No translation for CANDIDATE, so no label.
        assertFalse(model.contains(model.createResource(NS + "teslaris-position-47"),
            RDFS.label));
    }

    @Test
    public void shouldPreferRoleOverInvolvementType() {
        // When
        var model = mapper.toRdf(person(), target());

        // Then
        var position = model.createResource(NS + "teslaris-position-41");
        assertEquals(List.of("Teaching Assistant"),
            model.listObjectsOfProperty(position, RDFS.label).toList().stream()
                .map(label -> label.asLiteral().getString()).toList());
    }

    @Test
    public void shouldLinkResearchAreas() {
        // Given
        var person = person();
        person.setResearchAreaIds(new ArrayList<>(List.of(9, 4)));
        // When
        var mapping = mapper.map(person, target());

        // Then
        var model = mapping.model();
        var subject = model.createResource(NS + "teslaris-person-" + person.getDatabaseId());
        var area = model.createResource(NS + "teslaris-research-area-4");
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasResearchArea"), area));
        assertTrue(model.contains(area,
            model.createProperty("http://vivoweb.org/ontology/core#researchAreaOf"), subject));
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasResearchArea"),
            model.createResource(NS + "teslaris-research-area-9")));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.RESEARCH_AREAS, 4),
            new EntityReference(PushEntityType.RESEARCH_AREAS, 9))));
    }

    @Test
    public void shouldMapPastEmploymentAsAPositionWithAnEndDate() {
        // Given
        var person = person();
        person.setPastEmployments(new ArrayList<>(List.of(
            new ExportEmployment(organisationUnit(9), LocalDate.of(2015, 2, 1),
                LocalDate.of(2019, 12, 31), "Researcher", 77, InvolvementType.EMPLOYED_AT,
                new ArrayList<>()))));

        // When
        var model = mapper.toRdf(person, target());

        // Then
        var position = NS + "teslaris-position-77";
        assertTrue(has(model, position, RDF.type.getURI(), VIVO + "Position"));
        assertTrue(has(model, position, VIVO + "relates", NS + "teslaris-person-7"));
        assertTrue(has(model, position, VIVO + "relates", NS + "teslaris-ou-9"));
        assertTrue(model.contains(model.createResource(position), RDFS.label, "Researcher"));
        assertTrue(model.contains(model.createResource(position + "-end"),
            model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral("2019-12-31T12:00:00Z", XSDDatatype.XSDdateTime)));
    }

    @Test
    public void shouldMapEducationAsEducationalProcess() {
        // Given
        var person = person();
        var education = new ExportEducation(5, organisationUnit(2), new ArrayList<>(),
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "Computer science", 1))),
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "Summa cum laude", 1))),
            DegreeType.PHD, LocalDate.of(2016, 10, 1), LocalDate.of(2021, 6, 30));
        person.setEducations(new ArrayList<>(List.of(education)));

        // When
        var mapping = mapper.map(person, target());

        // Then
        var model = mapping.model();
        var personUri = NS + "teslaris-person-7";
        var process = personUri + "-education-5";
        var degree = NS + "teslaris-degree-phd";

        assertTrue(has(model, process, RDF.type.getURI(), VIVO + "EducationalProcess"));
        assertTrue(has(model, personUri, OBO + "RO_0000056", process));
        assertTrue(has(model, process, OBO + "RO_0000057", personUri));
        assertTrue(has(model, process, OBO + "RO_0000057", NS + "teslaris-ou-2"));
        assertTrue(has(model, NS + "teslaris-ou-2", OBO + "RO_0000056", process));
        assertTrue(model.contains(model.createResource(process),
            model.createProperty(VIVO + "majorField"),
            model.createLiteral("Computer science", "en")));
        assertTrue(model.contains(model.createResource(process),
            model.createProperty(VIVO + "supplementalInformation"),
            model.createLiteral("Summa cum laude", "en")));
        assertTrue(model.contains(model.createResource(process + "-start"),
            model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral("2016-10-01T12:00:00Z", XSDDatatype.XSDdateTime)));

        // The degree relationship belongs to the person, the degree itself is shared.
        var awardedDegree = process + "-degree";
        assertTrue(has(model, awardedDegree, RDF.type.getURI(), VIVO + "AwardedDegree"));
        assertTrue(has(model, awardedDegree, VIVO + "relates", degree));
        assertTrue(has(model, awardedDegree, VIVO + "relates", personUri));
        assertTrue(has(model, process, OBO + "RO_0002234", awardedDegree));
        assertFalse(model.contains(model.createResource(degree), RDFS.label));

        var node = mapping.sharedNodes().stream()
            .filter(sharedNode -> sharedNode.uri().equals(degree)).findFirst().orElseThrow();
        assertTrue(has(node.model(), degree, RDF.type.getURI(), VIVO + "AcademicDegree"));
        assertTrue(node.model().contains(node.model().createResource(degree), RDFS.label,
            node.model().createLiteral("Doctorate", "en")));
        assertTrue(node.model().contains(node.model().createResource(degree),
            node.model().createProperty(VIVO + "abbreviation"),
            node.model().createLiteral("PhD", "en")));
    }
}
