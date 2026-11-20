package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.FOAF;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.RdfMapper;

@Component
@RequiredArgsConstructor
public class VivoOrganisationUnitMapper implements RdfMapper<ExportOrganisationUnit> {

    private final VivoTeslarisLinks teslarisLinks;


    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportOrganisationUnit> sourceType() {
        return ExportOrganisationUnit.class;
    }

    @Override
    public Model toRdf(ExportOrganisationUnit snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var uri = VivoRdfSupport.organisationUnitUri(target, snapshot.getDatabaseId());
        var organisationUnit = model.createResource(uri);

        organisationUnit.addProperty(RDF.type, model.createResource(FOAF + "Organization"));
        VivoRdfSupport.addLiterals(model, organisationUnit, RDFS.label, snapshot.getName(),
            target);
        VivoRdfSupport.addLiterals(model, organisationUnit,
            model.createProperty(VIVO + "abbreviation"), snapshot.getNameAbbreviation(), target);

        var superOU = snapshot.getSuperOU();
        if (Objects.nonNull(superOU) && Objects.nonNull(superOU.getDatabaseId())) {
            var parent = model.createResource(
                VivoRdfSupport.organisationUnitUri(target, superOU.getDatabaseId()));
            organisationUnit.addProperty(model.createProperty(OBO + "BFO_0000050"), parent);
            parent.addProperty(model.createProperty(OBO + "BFO_0000051"), organisationUnit);
        }

        VivoRdfSupport.addCountry(model, organisationUnit, snapshot.getCountry());

        var links = VivoRdfSupport.withWebpages(links(snapshot), snapshot.getUris());
        if (!links.isEmpty()) {
            var vcard = VivoRdfSupport.addVcard(model, organisationUnit, uri, "Kind");
            VivoRdfSupport.addUrls(model, vcard, uri, links);
        }

        return model;
    }

    // VIVO has no organisation identifier properties, so the links show the identifiers.
    private List<Link> links(ExportOrganisationUnit snapshot) {
        var links = new ArrayList<Link>();
        links.add(teslarisLinks.profileLink("organisation-units", snapshot.getDatabaseId()));
        VivoRdfSupport.addIdentifierLink(links, "ror", "ROR", "https://ror.org/",
            snapshot.getRor());
        VivoRdfSupport.addIdentifierLink(links, "openalex", "OpenAlex", "https://openalex.org/",
            snapshot.getOpenAlex());
        VivoRdfSupport.addIdentifierLink(links, "scopus", "Scopus",
            "https://www.scopus.com/affil/profile.uri?afid=", snapshot.getScopusAfid());
        return links;
    }
}
