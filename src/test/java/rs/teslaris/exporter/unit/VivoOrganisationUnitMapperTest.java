package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.converter.push.vivo.VivoOrganisationUnitMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoOrganisationUnitMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    private static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private final VivoOrganisationUnitMapper mapper =
        new VivoOrganisationUnitMapper(
            new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"));


    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null,
            List.of("EN", "SR-CYR"), List.of("org_units"));
    }

    private static ExportOrganisationUnit organisationUnit(Integer id) {
        var organisationUnit = new ExportOrganisationUnit();
        organisationUnit.setDatabaseId(id);
        organisationUnit.setName(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "Faculty of Sciences", 1),
            new ExportMultilingualContent("SR-CYR", "Природно-математички факултет", 2),
            new ExportMultilingualContent("DE", "Fakultät", 3))));
        organisationUnit.setNameAbbreviation(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "PMF", 1))));
        return organisationUnit;
    }

    @Test
    public void shouldMapOrganisationUnitWithParentAndWebpages() {
        // Given
        var organisationUnit = organisationUnit(5);
        organisationUnit.setSuperOU(organisationUnit(1));
        organisationUnit.setUris(List.of("https://b.example", "https://a.example"));

        // When
        var model = mapper.toRdf(organisationUnit, target());

        // Then
        var subject = model.createResource(NS + "teslaris-ou-5");
        var parent = model.createResource(NS + "teslaris-ou-1");
        var vcard = model.createResource(NS + "teslaris-ou-5-vcard");
        var firstUrl = model.createResource(NS + "teslaris-ou-5-url-1");

        assertTrue(model.contains(subject, RDF.type,
            model.createResource("http://xmlns.com/foaf/0.1/Organization")));
        assertTrue(model.contains(subject, RDFS.label,
            model.createLiteral("Faculty of Sciences", "en")));
        assertTrue(model.contains(subject, RDFS.label,
            model.createLiteral("Природно-математички факултет", "sr-Cyrl")));
        assertFalse(model.contains(subject, RDFS.label, model.createLiteral("Fakultät", "de")));
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#abbreviation"),
            model.createLiteral("PMF", "en")));
        assertTrue(model.contains(subject, model.createProperty(OBO + "BFO_0000050"), parent));
        assertTrue(model.contains(parent, model.createProperty(OBO + "BFO_0000051"), subject));
        assertTrue(model.contains(subject, model.createProperty(OBO + "ARG_2000028"), vcard));
        assertTrue(model.contains(firstUrl, model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://a.example", XSDDatatype.XSDanyURI)));
    }

    @Test
    public void shouldMapIdentifiersAsLabelledLinksBeforeWebpages() {
        // Given
        var organisationUnit = organisationUnit(5);
        organisationUnit.setRor("05gq02987");
        organisationUnit.setScopusAfid("60068801");
        organisationUnit.setUris(List.of("https://ror.org/05gq02987", "https://uns.ac.rs"));

        // When
        var model = mapper.toRdf(organisationUnit, target());

        // Then
        var ror = model.createResource(NS + "teslaris-ou-5-url-ror");
        var scopus = model.createResource(NS + "teslaris-ou-5-url-scopus");
        var webpage = model.createResource(NS + "teslaris-ou-5-url-1");
        var rank = model.createProperty("http://vivoweb.org/ontology/core#rank");

        assertTrue(model.contains(ror, model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://ror.org/05gq02987", XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(ror, RDFS.label, "ROR"));
        var teslaris = model.createResource(NS + "teslaris-ou-5-url-teslaris");
        assertTrue(model.contains(teslaris, model.createProperty(VCARD + "url"),
            model.createTypedLiteral("http://localhost:5173/sr/organisation-units/5",
                XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(teslaris, RDFS.label, "CRIS UNS"));
        assertTrue(
            model.contains(teslaris, rank, model.createTypedLiteral(1, XSDDatatype.XSDint)));
        assertTrue(model.contains(ror, rank, model.createTypedLiteral(2, XSDDatatype.XSDint)));
        assertTrue(model.contains(scopus, model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://www.scopus.com/affil/profile.uri?afid=60068801",
                XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(webpage, model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://uns.ac.rs", XSDDatatype.XSDanyURI)));
        assertFalse(model.contains(model.createResource(NS + "teslaris-ou-5-url-2"), RDF.type));
        assertFalse(model.containsResource(
            model.createResource(NS + "teslaris-ou-5-url-openalex")));
    }

    @Test
    public void shouldMapOrganisationUnitWithoutParentAndWebpagesToTeslarisLinkOnly() {
        // Given
        var organisationUnit = organisationUnit(5);

        // When
        var model = mapper.toRdf(organisationUnit, target());

        // Then
        var subject = model.createResource(NS + "teslaris-ou-5");
        var vcard = model.createResource(NS + "teslaris-ou-5-vcard");
        assertFalse(model.contains(subject, model.createProperty(OBO + "BFO_0000050")));
        assertTrue(model.contains(subject, model.createProperty(OBO + "ARG_2000028"), vcard));
        assertEquals(1,
            model.listObjectsOfProperty(vcard, model.createProperty(VCARD + "hasURL")).toList()
                .size());
    }

    @Test
    public void shouldLinkKnownCountryAndSkipUnknownCountry() {
        // Given
        var serbian = organisationUnit(5);
        serbian.setCountry("RS");
        var unknown = organisationUnit(6);
        unknown.setCountry("XK");

        // When
        var serbianModel = mapper.toRdf(serbian, target());
        var unknownModel = mapper.toRdf(unknown, target());

        // Then
        var subject = serbianModel.createResource(NS + "teslaris-ou-5");
        var serbia =
            serbianModel.createResource("http://aims.fao.org/aos/geopolitical.owl#Serbia");
        assertTrue(serbianModel.contains(subject,
            serbianModel.createProperty(OBO + "RO_0001025"), serbia));
        assertTrue(serbianModel.contains(serbia,
            serbianModel.createProperty(OBO + "RO_0001015"), subject));
        assertFalse(unknownModel.contains(unknownModel.createResource(NS + "teslaris-ou-6"),
            unknownModel.createProperty(OBO + "RO_0001025")));
    }

    @Test
    public void shouldSerializeSameModelToSameSortedLines() {
        // Given
        var organisationUnit = organisationUnit(5);
        organisationUnit.setUris(List.of("https://b.example", "https://a.example"));

        // When
        var first = NTriplesSerializer.toLines(mapper.toRdf(organisationUnit, target()));
        var second = NTriplesSerializer.toLines(mapper.toRdf(organisationUnit, target()));

        // Then
        assertEquals(first, second);
        assertEquals(first.stream().sorted().toList(), first);
    }

    @Test
    public void shouldRejectBlankNodes() {
        // Given
        var model = ModelFactory.createDefaultModel();
        model.createResource().addProperty(RDFS.label, "blank");

        // When / Then
        assertThrows(IllegalStateException.class, () -> NTriplesSerializer.toLines(model));
    }
}
