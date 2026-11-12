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
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportEntityIdentifier;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.converter.push.vivo.VivoPersonMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoPersonMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private final VivoPersonMapper mapper = new VivoPersonMapper(
        new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"));


    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null, null,
            List.of("persons"));
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
                "Teaching Assistant", 41),
            new ExportEmployment(organisationUnit(3), null, null, null, 42),
            new ExportEmployment(organisationUnit(4), LocalDate.of(2019, 1, 1), null, "Old",
                null))));
        return person;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
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
            model.createTypedLiteral("2020-03-01T00:00:00", XSDDatatype.XSDdateTime)));
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
}
