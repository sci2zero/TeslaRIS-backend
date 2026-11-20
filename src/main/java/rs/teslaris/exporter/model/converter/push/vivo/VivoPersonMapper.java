package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.FOAF;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VCARD;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapping;
import rs.teslaris.exporter.util.push.SharedNode;

@Component
@RequiredArgsConstructor
public class VivoPersonMapper implements RdfMapper<ExportPerson> {

    private final VivoTeslarisLinks teslarisLinks;

    private final MessageSource messageSource;


    // VIVO shows person labels as "Last, First Middle".
    private static String label(ExportPersonName name) {
        var givenNames = Stream.of(name.getFirstName(), name.getMiddleName())
            .filter(StringUtil::valueExists).map(String::trim)
            .collect(Collectors.joining(" "));

        if (!StringUtil.valueExists(name.getLastName())) {
            return givenNames;
        }

        return givenNames.isEmpty() ? name.getLastName().trim() :
            name.getLastName().trim() + ", " + givenNames;
    }

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportPerson> sourceType() {
        return ExportPerson.class;
    }

    @Override
    public Model toRdf(ExportPerson snapshot, PushTarget target) {
        return map(snapshot, target).model();
    }

    @Override
    public RdfMapping map(ExportPerson snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var sharedNodes = new LinkedHashMap<String, SharedNode>();
        var uri = VivoRdfSupport.personUri(target, snapshot.getDatabaseId());
        var person = model.createResource(uri);

        person.addProperty(RDF.type, model.createResource(FOAF + "Person"));

        var vcard = VivoRdfSupport.addVcard(model, person, uri, "Individual");
        if (Objects.nonNull(snapshot.getName())) {
            var label = label(snapshot.getName());
            if (!label.isEmpty()) {
                person.addProperty(RDFS.label, label);
            }
            addName(model, vcard, uri, snapshot.getName());
        }

        addEmails(model, vcard, uri, snapshot.getElectronicAddresses());

        if (StringUtil.valueExists(snapshot.getOrcid())) {
            var orcid = snapshot.getOrcid().trim();
            person.addProperty(model.createProperty(VIVO + "orcidId"), model.createResource(
                orcid.startsWith("http") ? orcid : "https://orcid.org/" + orcid));
        }

        if (StringUtil.valueExists(snapshot.getScopusAuthorId())) {
            person.addProperty(model.createProperty(VIVO + "scopusId"),
                snapshot.getScopusAuthorId().trim());
        }

        VivoRdfSupport.addUrls(model, vcard, uri, links(snapshot));

        if (Objects.nonNull(snapshot.getEmployments())) {
            snapshot.getEmployments().forEach(
                employment -> addPosition(model, person, target, employment, sharedNodes));
        }

        return new RdfMapping(model, List.copyOf(sharedNodes.values()));
    }

    private void addName(Model model, Resource vcard, String uri, ExportPersonName name) {
        var vcardName = model.createResource(uri + "-vcard-name");
        vcardName.addProperty(RDF.type, model.createResource(VCARD + "Name"));
        vcard.addProperty(model.createProperty(VCARD + "hasName"), vcardName);

        if (StringUtil.valueExists(name.getFirstName())) {
            vcardName.addProperty(model.createProperty(VCARD + "givenName"),
                name.getFirstName().trim());
        }
        if (StringUtil.valueExists(name.getMiddleName())) {
            vcardName.addProperty(model.createProperty(VIVO + "middleName"),
                name.getMiddleName().trim());
        }
        if (StringUtil.valueExists(name.getLastName())) {
            vcardName.addProperty(model.createProperty(VCARD + "familyName"),
                name.getLastName().trim());
        }
    }

    private void addEmails(Model model, Resource vcard, String uri, List<String> emails) {
        if (Objects.isNull(emails)) {
            return;
        }

        var addresses = emails.stream().filter(StringUtil::valueExists).map(String::trim)
            .distinct().sorted().toList();
        for (int i = 0; i < addresses.size(); i++) {
            var email = model.createResource(uri + "-vcard-email-" + (i + 1));
            email.addProperty(RDF.type, model.createResource(VCARD + "Email"));
            email.addProperty(RDF.type, model.createResource(VCARD + "Work"));
            email.addProperty(model.createProperty(VCARD + "email"), addresses.get(i));
            vcard.addProperty(model.createProperty(VCARD + "hasEmail"), email);
        }
    }

    private List<Link> links(ExportPerson snapshot) {
        var links = new ArrayList<Link>();
        links.add(teslarisLinks.profileLink("persons", snapshot.getDatabaseId()));
        VivoRdfSupport.addIdentifierLink(links, "openalex", "OpenAlex", "https://openalex.org/",
            snapshot.getOpenAlexId());

        VivoRdfSupport.addIdentifierLinks(links, snapshot.getIdentifiers());
        return links;
    }

    // Adds one label per supported language, with the involvement type translated.
    private void addInvolvementTypeLabels(Model model, Resource position, PushTarget target,
                                          InvolvementType involvementType) {
        VivoRdfSupport.translations(messageSource, target,
                "involvementType." + involvementType.name())
            .forEach((languageTag, label) ->
                position.addProperty(RDFS.label, model.createLiteral(label, languageTag)));
        // Some types (for example CANDIDATE) have no translation, so they get no label.
    }

    // Returns null when the employment has no institution and no usable display name.
    private String organisationUnitUri(PushTarget target, ExportEmployment employment,
                                       Map<String, SharedNode> sharedNodes) {
        var institution = employment.getEmploymentInstitution();
        if (Objects.nonNull(institution)) {
            return Objects.nonNull(institution.getDatabaseId()) ?
                VivoRdfSupport.organisationUnitUri(target, institution.getDatabaseId()) : null;
        }

        // The person only links to this node. Its type and label are shared by all entities
        // that link to the same external institution.
        return VivoRdfSupport.externalOrganisation(target, employment.getDisplayOrganisationUnit(),
            sharedNodes).orElse(null);
    }

    private void addPosition(Model model, Resource person, PushTarget target,
                             ExportEmployment employment, Map<String, SharedNode> sharedNodes) {
        // Old snapshots do not have the involvement ID, so their positions are not sent.
        if (Objects.isNull(employment.getInvolvementId())) {
            return;
        }

        var organisationUnitUri = organisationUnitUri(target, employment, sharedNodes);
        if (Objects.isNull(organisationUnitUri)) {
            return;
        }

        var uri = VivoRdfSupport.positionUri(target, employment.getInvolvementId());
        var position = model.createResource(uri);
        var organisationUnit = model.createResource(organisationUnitUri);
        var relates = model.createProperty(VIVO + "relates");
        var relatedBy = model.createProperty(VIVO + "relatedBy");

        position.addProperty(RDF.type, model.createResource(VIVO + "Position"));
        if (StringUtil.valueExists(employment.getRole())) {
            position.addProperty(RDFS.label, employment.getRole().trim());
        } else if (Objects.nonNull(employment.getInvolvementType())) {
            addInvolvementTypeLabels(model, position, target, employment.getInvolvementType());
        }

        position.addProperty(relates, person);
        position.addProperty(relates, organisationUnit);
        person.addProperty(relatedBy, position);
        organisationUnit.addProperty(relatedBy, position);

        VivoRdfSupport.addDateTimeInterval(model, position, uri, employment.getDateFrom(),
            employment.getDateTo());
    }
}
