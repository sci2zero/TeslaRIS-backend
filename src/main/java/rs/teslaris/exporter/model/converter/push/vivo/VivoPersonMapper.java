package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.FOAF;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.person.DegreeType;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportEducation;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
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

        Stream.of(snapshot.getEmployments(), snapshot.getPastEmployments())
            .filter(Objects::nonNull).flatMap(List::stream).forEach(
                employment -> addPosition(model, person, target, employment, sharedNodes));

        if (Objects.nonNull(snapshot.getEducations())) {
            snapshot.getEducations().forEach(
                education -> addEducation(model, person, uri, target, education, sharedNodes));
        }

        var references = new ArrayList<EntityReference>();
        VivoRdfSupport.addResearchAreas(model, person, target, snapshot.getResearchAreaIds(),
            true, references);

        return new RdfMapping(model, List.copyOf(sharedNodes.values()), List.copyOf(references));
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

    /**
     * VIVO shows "education and training" on the person page: a vivo:EducationalProcess with the
     * person and the institution as participants, the degree through a vivo:AwardedDegree, and
     * the title as the major field.
     */
    private void addEducation(Model model, Resource person, String personUri, PushTarget target,
                              ExportEducation education, Map<String, SharedNode> sharedNodes) {
        if (Objects.isNull(education.getInvolvementId())) {
            return;
        }

        var uri = personUri + "-education-" + education.getInvolvementId();
        var process = model.createResource(uri);
        var participatesIn = model.createProperty(OBO + "RO_0000056");
        var hasParticipant = model.createProperty(OBO + "RO_0000057");

        process.addProperty(RDF.type, model.createResource(VIVO + "EducationalProcess"));
        VivoRdfSupport.addLiterals(model, process, RDFS.label, education.getTitle(), target);
        VivoRdfSupport.addLiterals(model, process,
            model.createProperty(VIVO + "majorField"), education.getTitle(), target);
        VivoRdfSupport.addLiterals(model, process,
            model.createProperty(VIVO + "supplementalInformation"),
            education.getDegreeClassification(), target);

        person.addProperty(participatesIn, process);
        process.addProperty(hasParticipant, person);

        var institutionUri = institutionUri(target, education, sharedNodes);
        if (Objects.nonNull(institutionUri)) {
            var institution = model.createResource(institutionUri);
            process.addProperty(hasParticipant, institution);
            institution.addProperty(participatesIn, process);
        }

        addAwardedDegree(model, process, person, uri, target, education.getDegreeType(),
            sharedNodes);

        VivoRdfSupport.addDateTimeInterval(model, process, uri, education.getDateFrom(),
            education.getDateTo());
    }

    // The degree itself is shared by every person with the same degree type.
    private void addAwardedDegree(Model model, Resource process, Resource person, String uri,
                                  PushTarget target, DegreeType degreeType,
                                  Map<String, SharedNode> sharedNodes) {
        if (Objects.isNull(degreeType)) {
            return;
        }

        var degreeUri = VivoRdfSupport.academicDegreeUri(target, degreeType);
        sharedNodes.computeIfAbsent(degreeUri, nodeUri -> {
            var nodeModel = ModelFactory.createDefaultModel();
            var degree = nodeModel.createResource(nodeUri);
            degree.addProperty(RDF.type, nodeModel.createResource(VIVO + "AcademicDegree"));
            addTranslations(nodeModel, degree, RDFS.label, target,
                "degreeType." + degreeType.name(), VivoProjectMapper.readable(degreeType));
            addTranslations(nodeModel, degree,
                nodeModel.createProperty(VIVO + "abbreviation"), target,
                "degreeTypeAbbreviation." + degreeType.name(), null);
            return new SharedNode(nodeUri, nodeModel);
        });

        var awardedDegree = model.createResource(uri + "-degree");
        var relates = model.createProperty(VIVO + "relates");
        awardedDegree.addProperty(RDF.type, model.createResource(VIVO + "AwardedDegree"));
        awardedDegree.addProperty(relates, model.createResource(degreeUri));
        awardedDegree.addProperty(relates, person);
        process.addProperty(model.createProperty(OBO + "RO_0002234"), awardedDegree);
        awardedDegree.addProperty(model.createProperty(OBO + "RO_0002353"), process);
    }

    private void addTranslations(Model model, Resource subject, Property property,
                                 PushTarget target, String code, String fallback) {
        var texts = VivoRdfSupport.translations(messageSource, target, code);
        if (texts.isEmpty()) {
            if (StringUtil.valueExists(fallback)) {
                subject.addProperty(property, fallback);
            }
            return;
        }

        texts.forEach((languageTag, text) -> subject.addProperty(property,
            model.createLiteral(text, languageTag)));
    }

    // Returns null when the education has no institution and no usable display name.
    private String institutionUri(PushTarget target, ExportEducation education,
                                  Map<String, SharedNode> sharedNodes) {
        var institution = education.getInstitution();
        if (Objects.nonNull(institution)) {
            return Objects.nonNull(institution.getDatabaseId()) ?
                VivoRdfSupport.organisationUnitUri(target, institution.getDatabaseId()) : null;
        }

        return VivoRdfSupport.externalOrganisation(target, education.getDisplayInstitution(),
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
