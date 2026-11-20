package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportFunding;
import rs.teslaris.exporter.model.common.ExportMonetaryAmount;
import rs.teslaris.exporter.model.common.ExportProjectDocument;
import rs.teslaris.exporter.model.common.ExportProjectOrganisationContribution;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapping;
import rs.teslaris.exporter.util.push.SharedNode;

/**
 * Maps a funding to vivo:Grant (vivo:Contract for contracts). The grant is assigned by the
 * funder and is the funding vehicle for its project. The project's principal investigators get a
 * vivo:PrincipalInvestigatorRole on the grant (VIVO's "principal investigator on" list), and its
 * coordinators a vivo:AdministratorRole ("administered by"). The persons and organisations come
 * from the project embedded in the funding snapshot.
 */
@Component
@RequiredArgsConstructor
public class VivoFundingMapper implements RdfMapper<ExportFunding> {

    private final VivoTeslarisLinks teslarisLinks;


    /**
     * The URI of a project organisation: its TeslaRIS OU, or the shared node of an external
     * organisation. Null when it has neither.
     */
    static String organisationUri(PushTarget target,
                                  ExportProjectOrganisationContribution contribution,
                                  Map<String, SharedNode> sharedNodes,
                                  List<EntityReference> references) {
        var organisationUnit = contribution.getOrganisationUnit();
        if (Objects.nonNull(organisationUnit) &&
            Objects.nonNull(organisationUnit.getDatabaseId())) {
            references.add(new EntityReference(PushEntityType.ORGANISATION_UNITS,
                organisationUnit.getDatabaseId()));
            return VivoRdfSupport.organisationUnitUri(target, organisationUnit.getDatabaseId());
        }

        return VivoRdfSupport.externalOrganisation(target, contribution.getDisplayName(),
            sharedNodes).orElse(null);
    }

    // VIVO stores the amount as text, for example "125000.00 EUR".
    static String amountText(ExportMonetaryAmount amount) {
        var number = String.format(Locale.ROOT, "%.2f", amount.getAmount());
        return StringUtil.valueExists(amount.getCurrencyCode()) ?
            number + " " + amount.getCurrencyCode().trim() : number;
    }

    /**
     * Whether this funding is credited with the output. With funding parts, only the fundings
     * named there are. Without funding parts:
     * <ul>
     *     <li>{@code creditAllWithoutParts = true}: every funding of the project is credited
     *     (it is not known which one paid, so possibly more than the real ones);</li>
     *     <li>{@code false}: only when the project has exactly one funding (this one). Older
     *     snapshots have no funding count, so they are not credited.</li>
     * </ul>
     */
    static boolean supports(ExportFunding funding, ExportProjectDocument output,
                            boolean creditAllWithoutParts) {
        if (Objects.nonNull(output.getFundingIds()) && !output.getFundingIds().isEmpty()) {
            return output.getFundingIds().contains(funding.getDatabaseId());
        }

        if (creditAllWithoutParts) {
            return true;
        }

        var project = funding.getProject();
        return Objects.nonNull(project) && Objects.nonNull(project.getFundingCount()) &&
            project.getFundingCount() == 1;
    }

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportFunding> sourceType() {
        return ExportFunding.class;
    }

    @Override
    public Model toRdf(ExportFunding snapshot, PushTarget target) {
        return map(snapshot, target).model();
    }

    @Override
    public RdfMapping map(ExportFunding snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var sharedNodes = new LinkedHashMap<String, SharedNode>();
        var references = new ArrayList<EntityReference>();

        var uri = VivoRdfSupport.fundingUri(target, snapshot.getDatabaseId());
        var grant = model.createResource(uri);

        var isContract = Objects.nonNull(snapshot.getFundingTypes()) &&
            snapshot.getFundingTypes().contains(FundingType.CONTRACT);
        grant.addProperty(RDF.type,
            model.createResource(VIVO + (isContract ? "Contract" : "Grant")));

        VivoRdfSupport.addLiterals(model, grant, RDFS.label, snapshot.getName(), target);
        VivoRdfSupport.addLiterals(model, grant, model.createProperty(VIVO + "abbreviation"),
            snapshot.getNameAbbreviation(), target);
        VivoRdfSupport.addLiterals(model, grant, model.createProperty(VIVO + "description"),
            snapshot.getDescription(), target);
        VivoRdfSupport.addKeywords(model, grant, snapshot.getKeywords(), target);
        VivoRdfSupport.addDateTimeInterval(model, grant, uri, snapshot.getDateFrom(),
            snapshot.getDateTo());

        if (StringUtil.valueExists(snapshot.getGrantAgreementId())) {
            grant.addProperty(model.createProperty(VIVO + "sponsorAwardId"),
                snapshot.getGrantAgreementId().trim());
        }
        if (Objects.nonNull(snapshot.getAmount())) {
            grant.addProperty(model.createProperty(VIVO + "totalAwardAmount"),
                amountText(snapshot.getAmount()));
        }

        addFunder(model, grant, target, snapshot, sharedNodes, references);
        addProject(model, grant, target, snapshot, references);
        addPrincipalInvestigators(model, grant, uri, target, snapshot, references);
        addAdministrators(model, grant, uri, target, snapshot, sharedNodes, references);
        addSupportedOutputs(model, grant, target, snapshot, references);

        var links = VivoRdfSupport.withWebpages(links(snapshot), snapshot.getUris());
        if (!links.isEmpty()) {
            var vcard = VivoRdfSupport.addVcard(model, grant, uri, "Kind");
            VivoRdfSupport.addUrls(model, vcard, uri, links);
        }

        return new RdfMapping(model, List.copyOf(sharedNodes.values()), List.copyOf(references));
    }

    // VIVO shows it on the organisation page as "awards grant".
    private void addFunder(Model model, Resource grant, PushTarget target,
                           ExportFunding snapshot, Map<String, SharedNode> sharedNodes,
                           List<EntityReference> references) {
        String funderUri = null;

        var funder = snapshot.getFunder();
        if (Objects.nonNull(funder) && Objects.nonNull(funder.getDatabaseId())) {
            funderUri = VivoRdfSupport.organisationUnitUri(target, funder.getDatabaseId());
            references.add(new EntityReference(PushEntityType.ORGANISATION_UNITS,
                funder.getDatabaseId()));
        } else {
            // Same node as an external institution with this name.
            funderUri = VivoRdfSupport.externalOrganisation(target, snapshot.getDisplayFunder(),
                sharedNodes).orElse(null);
        }

        if (Objects.isNull(funderUri)) {
            return;
        }

        var funderResource = model.createResource(funderUri);
        grant.addProperty(model.createProperty(VIVO + "assignedBy"), funderResource);
        funderResource.addProperty(model.createProperty(VIVO + "assigns"), grant);
    }

    private void addProject(Model model, Resource grant, PushTarget target,
                            ExportFunding snapshot, List<EntityReference> references) {
        var project = snapshot.getProject();
        if (Objects.isNull(project) || Objects.isNull(project.getDatabaseId())) {
            return;
        }

        var projectResource =
            model.createResource(VivoRdfSupport.projectUri(target, project.getDatabaseId()));
        grant.addProperty(model.createProperty(VIVO + "fundingVehicleFor"), projectResource);
        projectResource.addProperty(model.createProperty(VIVO + "hasFundingVehicle"), grant);
        references.add(new EntityReference(PushEntityType.PROJECTS, project.getDatabaseId()));
    }

    /**
     * "{funding}-pi-{n}" for each principal investigator of the project. A PI without a TeslaRIS
     * person is a vcard:Individual with the display name; VIVO shows it in the grant's
     * contributor list.
     */
    private void addPrincipalInvestigators(Model model, Resource grant, String uri,
                                           PushTarget target, ExportFunding snapshot,
                                           List<EntityReference> references) {
        var investigators = VivoProjectMapper.sortedPersons(snapshot.getProject()).stream()
            .filter(contribution -> contribution.getContributionType() ==
                PersonProjectContributionType.PRINCIPLE_INVESTIGATOR)
            .toList();

        var relates = model.createProperty(VIVO + "relates");
        var relatedBy = model.createProperty(VIVO + "relatedBy");
        var inheresIn = model.createProperty(OBO + "RO_0000052");
        var bearerOf = model.createProperty(OBO + "RO_0000053");

        for (int i = 0; i < investigators.size(); i++) {
            var contribution = investigators.get(i);
            var roleUri = uri + "-pi-" + (i + 1);

            Resource bearer;
            var person = contribution.getPerson();
            if (Objects.nonNull(person) && Objects.nonNull(person.getDatabaseId())) {
                bearer = model.createResource(
                    VivoRdfSupport.personUri(target, person.getDatabaseId()));
                grant.addProperty(relates, bearer);
                bearer.addProperty(relatedBy, grant);
                references.add(
                    new EntityReference(PushEntityType.PERSONS, person.getDatabaseId()));
            } else if (StringUtil.valueExists(contribution.getDisplayName())) {
                bearer = VivoRdfSupport.addExternalPersonVcard(model, roleUri,
                    contribution.getDisplayName());
            } else {
                continue;
            }

            var role = model.createResource(roleUri);
            role.addProperty(RDF.type, model.createResource(VIVO + "PrincipalInvestigatorRole"));
            grant.addProperty(relates, role);
            role.addProperty(relatedBy, grant);
            role.addProperty(inheresIn, bearer);
            bearer.addProperty(bearerOf, role);

            VivoRdfSupport.addDateTimeInterval(model, role, roleUri, contribution.getDateFrom(),
                contribution.getDateTo());
        }
    }

    // "{funding}-admin-{n}" for each coordinator of the project ("administered by").
    private void addAdministrators(Model model, Resource grant, String uri, PushTarget target,
                                   ExportFunding snapshot, Map<String, SharedNode> sharedNodes,
                                   List<EntityReference> references) {
        var coordinators = VivoProjectMapper.sortedOrganisations(snapshot.getProject()).stream()
            .filter(contribution -> contribution.getContributionType() ==
                OrganisationUnitProjectContributionType.COORDINATOR)
            .toList();

        for (int i = 0; i < coordinators.size(); i++) {
            var organisationUri =
                organisationUri(target, coordinators.get(i), sharedNodes, references);
            if (Objects.isNull(organisationUri)) {
                continue;
            }

            var organisation = model.createResource(organisationUri);
            var role = model.createResource(uri + "-admin-" + (i + 1));
            role.addProperty(RDF.type, model.createResource(VIVO + "AdministratorRole"));
            grant.addProperty(model.createProperty(VIVO + "relates"), role);
            role.addProperty(model.createProperty(VIVO + "relatedBy"), grant);
            role.addProperty(model.createProperty(OBO + "RO_0000052"), organisation);
            organisation.addProperty(model.createProperty(OBO + "RO_0000053"), role);
        }
    }

    // VIVO: "supported publications or other works" on the grant, "supported by" on the
    // document. The outputs come from the project embedded in the funding snapshot.
    private void addSupportedOutputs(Model model, Resource grant, PushTarget target,
                                     ExportFunding snapshot, List<EntityReference> references) {
        var project = snapshot.getProject();
        if (Objects.isNull(project) || Objects.isNull(project.getDocuments())) {
            return;
        }

        project.getDocuments().stream()
            .filter(output -> Objects.nonNull(output.getDocumentId()) &&
                VivoProjectMapper.OUTPUT_RELATION_TYPES.contains(output.getRelationType()))
            // Per push target: "creditAllFundingsWithoutFundingParts" (default true).
            .filter(output -> supports(snapshot, output,
                target.creditsAllFundingsWithoutFundingParts()))
            .forEach(output -> {
                var document = model.createResource(
                    VivoRdfSupport.documentUri(target, output.getDocumentId()));
                grant.addProperty(model.createProperty(VIVO + "supportedInformationResource"),
                    document);
                document.addProperty(
                    model.createProperty(VIVO + "informationResourceSupportedBy"), grant);
                references.add(
                    new EntityReference(PushEntityType.DOCUMENTS, output.getDocumentId()));
            });
    }

    private List<Link> links(ExportFunding snapshot) {
        var links = new ArrayList<Link>();
        links.add(teslarisLinks.profileLink("funding", snapshot.getDatabaseId()));
        VivoRdfSupport.addIdentifierLink(links, "doi", "DOI", "https://doi.org/",
            snapshot.getDoi());
        VivoRdfSupport.addIdentifierLinks(links, snapshot.getIdentifiers());
        return links;
    }
}
