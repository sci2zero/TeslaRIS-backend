package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.SKOS;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.exporter.model.common.ExportResearchArea;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapping;
import rs.teslaris.exporter.util.push.SharedNode;
import rs.teslaris.exporter.util.push.SharedNodeKey;

/**
 * Maps a research area to skos:Concept. The child owns skos:broader / skos:narrower to its
 * parent. The scheme is a shared node made from the scheme name, typed skos:ConceptScheme. A
 * root area also writes skos:topConceptOf / skos:hasTopConcept.
 * <p>
 * Stock VIVO 1.15 shows the label, vivo:description and the hierarchy. skos:ConceptScheme,
 * skos:inScheme and the top concept properties are not in VIVO's ontology or public display
 * lists, so they are not shown until VIVO is configured for them.
 */
@Component
public class VivoResearchAreaMapper implements RdfMapper<ExportResearchArea> {

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportResearchArea> sourceType() {
        return ExportResearchArea.class;
    }

    @Override
    public Model toRdf(ExportResearchArea snapshot, PushTarget target) {
        return map(snapshot, target).model();
    }

    @Override
    public RdfMapping map(ExportResearchArea snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var sharedNodes = new ArrayList<SharedNode>();
        var references = new ArrayList<EntityReference>();

        var concept =
            model.createResource(VivoRdfSupport.researchAreaUri(target, snapshot.getDatabaseId()));
        concept.addProperty(RDF.type, model.createResource(SKOS + "Concept"));
        VivoRdfSupport.addLiterals(model, concept, RDFS.label, snapshot.getName(), target);
        VivoRdfSupport.addLiterals(model, concept, model.createProperty(VIVO + "description"),
            snapshot.getDescription(), target);

        var isRoot = Objects.isNull(snapshot.getSuperResearchAreaId());
        if (!isRoot) {
            var parent = model.createResource(
                VivoRdfSupport.researchAreaUri(target, snapshot.getSuperResearchAreaId()));
            concept.addProperty(model.createProperty(SKOS + "broader"), parent);
            parent.addProperty(model.createProperty(SKOS + "narrower"), concept);
            references.add(new EntityReference(PushEntityType.RESEARCH_AREAS,
                snapshot.getSuperResearchAreaId()));
        }

        SharedNodeKey.of(snapshot.getSchemeName()).ifPresent(key -> {
            var schemeUri = VivoRdfSupport.conceptSchemeUri(target, key);
            var nodeModel = ModelFactory.createDefaultModel();
            nodeModel.createResource(schemeUri)
                .addProperty(RDF.type, nodeModel.createResource(SKOS + "ConceptScheme"))
                .addProperty(RDFS.label, snapshot.getSchemeName().trim());
            sharedNodes.add(new SharedNode(schemeUri, nodeModel));

            var scheme = model.createResource(schemeUri);
            concept.addProperty(model.createProperty(SKOS + "inScheme"), scheme);
            if (isRoot) {
                concept.addProperty(model.createProperty(SKOS + "topConceptOf"), scheme);
                scheme.addProperty(model.createProperty(SKOS + "hasTopConcept"), concept);
            }
        });

        return new RdfMapping(model, List.copyOf(sharedNodes), List.copyOf(references));
    }
}
