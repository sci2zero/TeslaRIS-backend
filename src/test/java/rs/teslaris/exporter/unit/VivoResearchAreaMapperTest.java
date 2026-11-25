package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportResearchArea;
import rs.teslaris.exporter.model.converter.push.vivo.VivoResearchAreaMapper;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoResearchAreaMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String URI = NS + "teslaris-research-area-7";

    private final VivoResearchAreaMapper mapper = new VivoResearchAreaMapper();


    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null,
            List.of("EN", "SR"), List.of("research_areas"), null);
    }

    private static ExportResearchArea researchArea(Integer parentId, String schemeName) {
        var researchArea = new ExportResearchArea();
        researchArea.setDatabaseId(7);
        researchArea.setName(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "Computer science", 1),
            new ExportMultilingualContent("SR", "Računarstvo", 2))));
        researchArea.setDescription(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "Study of computation", 1))));
        researchArea.setSuperResearchAreaId(parentId);
        researchArea.setSchemeName(schemeName);
        return researchArea;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
    }

    @Test
    public void shouldMapConceptWithLabelsDescriptionAndParent() {
        // When
        var mapping = mapper.map(researchArea(3, null), target());

        // Then
        var model = mapping.model();
        var concept = model.createResource(URI);
        var parent = NS + "teslaris-research-area-3";
        assertTrue(has(model, URI, RDF.type.getURI(), SKOS + "Concept"));
        assertTrue(model.contains(concept, RDFS.label,
            model.createLiteral("Computer science", "en")));
        assertTrue(model.contains(concept, RDFS.label, model.createLiteral("Računarstvo", "sr")));
        assertTrue(model.contains(concept, model.createProperty(VIVO + "description"),
            model.createLiteral("Study of computation", "en")));
        assertTrue(has(model, URI, SKOS + "broader", parent));
        assertTrue(has(model, parent, SKOS + "narrower", URI));
        assertFalse(model.contains(concept, model.createProperty(SKOS + "topConceptOf")));
        assertEquals(List.of(new EntityReference(PushEntityType.RESEARCH_AREAS, 3)),
            mapping.references());
        assertTrue(mapping.sharedNodes().isEmpty());
        NTriplesSerializer.toLines(model);
    }

    @Test
    public void shouldLinkChildToSchemeAsSharedNode() {
        // When
        var mapping = mapper.map(researchArea(3, "ERC"), target());

        // Then
        var node = mapping.sharedNodes().getFirst();
        assertTrue(node.uri().startsWith(NS + "teslaris-concept-scheme-"));
        assertFalse(node.replaceWhenChanged());
        assertTrue(has(node.model(), node.uri(), RDF.type.getURI(), SKOS + "ConceptScheme"));
        assertTrue(node.model().contains(node.model().createResource(node.uri()), RDFS.label,
            "ERC"));
        assertTrue(has(mapping.model(), URI, SKOS + "inScheme", node.uri()));
        assertFalse(has(mapping.model(), URI, SKOS + "topConceptOf", node.uri()));
    }

    @Test
    public void shouldMakeRootTopConceptOfScheme() {
        // When
        var mapping = mapper.map(researchArea(null, "ERC"), target());

        // Then
        var model = mapping.model();
        var schemeUri = mapping.sharedNodes().getFirst().uri();
        assertTrue(has(model, URI, SKOS + "inScheme", schemeUri));
        assertTrue(has(model, URI, SKOS + "topConceptOf", schemeUri));
        assertTrue(has(model, schemeUri, SKOS + "hasTopConcept", URI));
        assertFalse(model.contains(model.createResource(URI),
            model.createProperty(SKOS + "broader")));
        assertTrue(mapping.references().isEmpty());
    }

    @Test
    public void shouldGiveSameSchemeUriForSameNormalisedName() {
        // When
        var first = mapper.map(researchArea(null, "ERC Panels"), target());
        var second = mapper.map(researchArea(null, "  erc   panels "), target());

        // Then
        assertEquals(first.sharedNodes().getFirst().uri(),
            second.sharedNodes().getFirst().uri());
    }

    @Test
    public void shouldSendNoSchemeForBlankSchemeName() {
        // When
        var mapping = mapper.map(researchArea(null, " "), target());

        // Then
        assertTrue(mapping.sharedNodes().isEmpty());
        assertFalse(mapping.model().contains(mapping.model().createResource(URI),
            mapping.model().createProperty(SKOS + "inScheme")));
    }
}
