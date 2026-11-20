package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.exporter.model.common.ExportProject;
import rs.teslaris.exporter.model.common.ExportProjectOrganisationContribution;
import rs.teslaris.exporter.model.common.ExportProjectPersonContribution;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapping;
import rs.teslaris.exporter.util.push.SharedNode;

/**
 * Maps a project to vivo:Project. Each person with a TeslaRIS profile gets a vivo:ResearcherRole
 * realized in the project, which VIVO shows on the person page as "other research activities".
 * Organisations are linked with "has participant". The grant side (funder, principal
 * investigators, coordinators) is written by {@link VivoFundingMapper}.
 */
@Component
@RequiredArgsConstructor
public class VivoProjectMapper implements RdfMapper<ExportProject> {

    // ProjectDocumentType names that are outputs of the project. USED (an input) and INTERNAL
    // are not sent.
    static final Set<String> OUTPUT_RELATION_TYPES = Set.of("RESULT", "REPORT");

    private final VivoTeslarisLinks teslarisLinks;

    private final MessageSource messageSource;


    // "TEAM_MEMBER" → "Team member", for codes without a translation.
    static String readable(Enum<?> value) {
        var text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static List<ExportProjectPersonContribution> sortedPersons(ExportProject project) {
        if (Objects.isNull(project) || Objects.isNull(project.getPersons())) {
            return List.of();
        }

        return project.getPersons().stream().filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(ExportProjectPersonContribution::getOrderNumber))
            .toList();
    }

    static List<ExportProjectOrganisationContribution> sortedOrganisations(
        ExportProject project) {
        if (Objects.isNull(project) || Objects.isNull(project.getOrganisations())) {
            return List.of();
        }

        return project.getOrganisations().stream().filter(Objects::nonNull)
            .sorted(Comparator.comparing(ExportProjectOrganisationContribution::getOrderNumber,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportProject> sourceType() {
        return ExportProject.class;
    }

    @Override
    public Model toRdf(ExportProject snapshot, PushTarget target) {
        return map(snapshot, target).model();
    }

    @Override
    public RdfMapping map(ExportProject snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var sharedNodes = new LinkedHashMap<String, SharedNode>();
        var references = new ArrayList<EntityReference>();

        var uri = VivoRdfSupport.projectUri(target, snapshot.getDatabaseId());
        var project = model.createResource(uri);

        project.addProperty(RDF.type, model.createResource(VIVO + "Project"));
        VivoRdfSupport.addLiterals(model, project, RDFS.label, snapshot.getName(), target);
        VivoRdfSupport.addLiterals(model, project, model.createProperty(VIVO + "abbreviation"),
            snapshot.getNameAbbreviation(), target);
        VivoRdfSupport.addLiterals(model, project, model.createProperty(VIVO + "description"),
            snapshot.getDescription(), target);
        VivoRdfSupport.addKeywords(model, project, snapshot.getKeywords(), target);
        VivoRdfSupport.addDateTimeInterval(model, project, uri, snapshot.getDateFrom(),
            snapshot.getDateTo());

        addResearcherRoles(model, project, uri, target, snapshot, references);
        addParticipants(model, project, target, snapshot, sharedNodes, references);
        addOutputs(model, project, target, snapshot, references);

        var links = VivoRdfSupport.withWebpages(links(snapshot), snapshot.getUris());
        if (!links.isEmpty()) {
            var vcard = VivoRdfSupport.addVcard(model, project, uri, "Kind");
            VivoRdfSupport.addUrls(model, vcard, uri, links);
        }

        return new RdfMapping(model, List.copyOf(sharedNodes.values()), List.copyOf(references));
    }

    /**
     * One role per contribution, "{project}-role-{n}", where n is the position in the list sorted
     * by the order number. Contributions without a TeslaRIS person are not sent: the role would
     * show only on the person's own page, and they have none.
     */
    private void addResearcherRoles(Model model, Resource project, String uri, PushTarget target,
                                    ExportProject snapshot, List<EntityReference> references) {
        var persons = sortedPersons(snapshot);

        for (int i = 0; i < persons.size(); i++) {
            var contribution = persons.get(i);
            var person = contribution.getPerson();
            if (Objects.isNull(person) || Objects.isNull(person.getDatabaseId())) {
                continue;
            }

            var roleUri = uri + "-role-" + (i + 1);
            var role = model.createResource(roleUri);
            var personResource =
                model.createResource(VivoRdfSupport.personUri(target, person.getDatabaseId()));

            role.addProperty(RDF.type, model.createResource(VIVO + "ResearcherRole"));
            roleLabels(target, contribution).forEach((languageTag, label) ->
                role.addProperty(RDFS.label, languageTag.isEmpty() ? model.createLiteral(label) :
                    model.createLiteral(label, languageTag)));

            role.addProperty(model.createProperty(OBO + "RO_0000052"), personResource);
            personResource.addProperty(model.createProperty(OBO + "RO_0000053"), role);
            role.addProperty(model.createProperty(OBO + "BFO_0000054"), project);
            project.addProperty(model.createProperty(OBO + "BFO_0000055"), role);

            VivoRdfSupport.addDateTimeInterval(model, role, roleUri, contribution.getDateFrom(),
                contribution.getDateTo());
            references.add(new EntityReference(PushEntityType.PERSONS, person.getDatabaseId()));
        }
    }

    /**
     * "Team member (PhD student fellow)" per language. Without translations (for example when no
     * supported language has a messages file), one label without a language tag, from the enum
     * names. The key "" means no language tag.
     */
    Map<String, String> roleLabels(PushTarget target,
                                   ExportProjectPersonContribution contribution) {
        var labels = new LinkedHashMap<String, String>();
        var type = contribution.getContributionType();
        var investigationRole = contribution.getInvestigationRole();
        if (Objects.isNull(type) && Objects.isNull(investigationRole)) {
            return labels;
        }

        var typeTexts = Objects.nonNull(type) ? VivoRdfSupport.translations(messageSource,
            target, "personProjectContributionType." + type.name()) : Map.<String, String>of();
        var roleTexts = Objects.nonNull(investigationRole) ?
            VivoRdfSupport.translations(messageSource, target,
                "personProjectInvestigationRole." + investigationRole.name()) :
            Map.<String, String>of();

        var languageTags = new ArrayList<String>(typeTexts.keySet());
        roleTexts.keySet().stream().filter(tag -> !languageTags.contains(tag))
            .forEach(languageTags::add);

        if (languageTags.isEmpty()) {
            labels.put("", join(Objects.nonNull(type) ? readable(type) : null,
                Objects.nonNull(investigationRole) ? readable(investigationRole) : null));
            return labels;
        }

        languageTags.forEach(tag -> labels.put(tag, join(
            Objects.nonNull(type) ? typeTexts.getOrDefault(tag, readable(type)) : null,
            Objects.nonNull(investigationRole) ?
                roleTexts.getOrDefault(tag, readable(investigationRole)) : null)));
        return labels;
    }

    private static String join(String type, String investigationRole) {
        if (Objects.isNull(type)) {
            return investigationRole;
        }

        return Objects.isNull(investigationRole) ? type : type + " (" + investigationRole + ")";
    }

    // VIVO has no organisation role that shows on a project, so the plain "has participant" /
    // "participates in" pair is used. External organisations are shared nodes.
    private void addParticipants(Model model, Resource project, PushTarget target,
                                 ExportProject snapshot, Map<String, SharedNode> sharedNodes,
                                 List<EntityReference> references) {
        sortedOrganisations(snapshot).forEach(contribution -> {
            var organisationUri =
                VivoFundingMapper.organisationUri(target, contribution, sharedNodes, references);
            if (Objects.isNull(organisationUri)) {
                return;
            }

            var organisation = model.createResource(organisationUri);
            project.addProperty(model.createProperty(OBO + "RO_0000057"), organisation);
            organisation.addProperty(model.createProperty(OBO + "RO_0000056"), project);
        });
    }

    /**
     * VIVO shows obo:RO_0002234 on the project as "output publications or other works" and
     * obo:RO_0002353 on the document as "output of process or event" (faux properties for a
     * process and an information content entity). The project owns both triples.
     */
    private void addOutputs(Model model, Resource project, PushTarget target,
                            ExportProject snapshot, List<EntityReference> references) {
        if (Objects.isNull(snapshot.getDocuments())) {
            return;
        }

        snapshot.getDocuments().stream()
            .filter(link -> Objects.nonNull(link.getDocumentId()) &&
                OUTPUT_RELATION_TYPES.contains(link.getRelationType()))
            .forEach(link -> {
                var document = model.createResource(
                    VivoRdfSupport.documentUri(target, link.getDocumentId()));
                project.addProperty(model.createProperty(OBO + "RO_0002234"), document);
                document.addProperty(model.createProperty(OBO + "RO_0002353"), project);
                references.add(
                    new EntityReference(PushEntityType.DOCUMENTS, link.getDocumentId()));
            });
    }

    private List<Link> links(ExportProject snapshot) {
        var links = new ArrayList<Link>();
        links.add(teslarisLinks.profileLink("project", snapshot.getDatabaseId()));
        VivoRdfSupport.addIdentifierLink(links, "doi", "DOI", "https://doi.org/",
            snapshot.getDoi());
        VivoRdfSupport.addIdentifierLink(links, "raid", "RAiD", "https://raid.org/",
            snapshot.getRaid());
        VivoRdfSupport.addIdentifierLinks(links, snapshot.getIdentifiers());
        return links;
    }
}
