package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.FOAF;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VCARD;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.RdfMapper;

@Component
@RequiredArgsConstructor
public class VivoPersonMapper implements RdfMapper<ExportPerson> {

    private final VivoTeslarisLinks teslarisLinks;


    private static boolean hasText(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    // VIVO shows person labels as "Last, First Middle".
    private static String label(ExportPersonName name) {
        var givenNames = Stream.of(name.getFirstName(), name.getMiddleName())
            .filter(VivoPersonMapper::hasText).map(String::trim)
            .collect(Collectors.joining(" "));

        if (!hasText(name.getLastName())) {
            return givenNames;
        }

        return givenNames.isEmpty() ? name.getLastName().trim() :
            name.getLastName().trim() + ", " + givenNames;
    }

    private static String keyOf(String code) {
        return code.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
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
        var model = ModelFactory.createDefaultModel();
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

        if (hasText(snapshot.getOrcid())) {
            var orcid = snapshot.getOrcid().trim();
            person.addProperty(model.createProperty(VIVO + "orcidId"), model.createResource(
                orcid.startsWith("http") ? orcid : "https://orcid.org/" + orcid));
        }

        if (hasText(snapshot.getScopusAuthorId())) {
            person.addProperty(model.createProperty(VIVO + "scopusId"),
                snapshot.getScopusAuthorId().trim());
        }

        VivoRdfSupport.addUrls(model, vcard, uri, links(snapshot));

        if (Objects.nonNull(snapshot.getEmployments())) {
            snapshot.getEmployments()
                .forEach(employment -> addPosition(model, person, target, employment));
        }

        return model;
    }

    private void addName(Model model, Resource vcard, String uri, ExportPersonName name) {
        var vcardName = model.createResource(uri + "-vcard-name");
        vcardName.addProperty(RDF.type, model.createResource(VCARD + "Name"));
        vcard.addProperty(model.createProperty(VCARD + "hasName"), vcardName);

        if (hasText(name.getFirstName())) {
            vcardName.addProperty(model.createProperty(VCARD + "givenName"),
                name.getFirstName().trim());
        }
        if (hasText(name.getMiddleName())) {
            vcardName.addProperty(model.createProperty(VIVO + "middleName"),
                name.getMiddleName().trim());
        }
        if (hasText(name.getLastName())) {
            vcardName.addProperty(model.createProperty(VCARD + "familyName"),
                name.getLastName().trim());
        }
    }

    private void addEmails(Model model, Resource vcard, String uri, List<String> emails) {
        if (Objects.isNull(emails)) {
            return;
        }

        var addresses = emails.stream().filter(VivoPersonMapper::hasText).map(String::trim)
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

        if (Objects.isNull(snapshot.getIdentifiers())) {
            return links;
        }

        var keys = new HashSet<String>();
        links.forEach(link -> keys.add(link.key()));
        snapshot.getIdentifiers().stream()
            .filter(identifier -> hasText(identifier.getCode()))
            .sorted((first, second) -> {
                var byCode = first.getCode().compareTo(second.getCode());
                return byCode != 0 ? byCode :
                    String.valueOf(first.getValue()).compareTo(String.valueOf(second.getValue()));
            })
            .forEach(identifier -> {
                var url = IdentifierUtil.buildIdentifierUrl(identifier.getUrlTemplate(),
                    identifier.getValue());
                if (Objects.isNull(url) ||
                    links.stream().anyMatch(link -> link.url().equals(url))) {
                    return;
                }

                var key = keyOf(identifier.getCode());
                for (int i = 2; keys.contains(key); i++) {
                    key = keyOf(identifier.getCode()) + "-" + i;
                }
                keys.add(key);
                links.add(new Link(key, identifier.getCode(), url));
            });

        return links;
    }

    private void addPosition(Model model, Resource person, PushTarget target,
                             ExportEmployment employment) {
        var institution = employment.getEmploymentInstitution();
        // Old snapshots do not have the involvement ID, so their positions are not sent.
        if (Objects.isNull(employment.getInvolvementId()) || Objects.isNull(institution) ||
            Objects.isNull(institution.getDatabaseId())) {
            return;
        }

        var uri = VivoRdfSupport.positionUri(target, employment.getInvolvementId());
        var position = model.createResource(uri);
        var organisationUnit = model.createResource(
            VivoRdfSupport.organisationUnitUri(target, institution.getDatabaseId()));
        var relates = model.createProperty(VIVO + "relates");
        var relatedBy = model.createProperty(VIVO + "relatedBy");

        position.addProperty(RDF.type, model.createResource(VIVO + "Position"));
        if (hasText(employment.getRole())) {
            position.addProperty(RDFS.label, employment.getRole().trim());
        }

        position.addProperty(relates, person);
        position.addProperty(relates, organisationUnit);
        person.addProperty(relatedBy, position);
        organisationUnit.addProperty(relatedBy, position);

        if (Objects.isNull(employment.getDateFrom()) && Objects.isNull(employment.getDateTo())) {
            return;
        }

        var interval = model.createResource(uri + "-interval");
        interval.addProperty(RDF.type, model.createResource(VIVO + "DateTimeInterval"));
        position.addProperty(model.createProperty(VIVO + "dateTimeInterval"), interval);

        if (Objects.nonNull(employment.getDateFrom())) {
            interval.addProperty(model.createProperty(VIVO + "start"),
                VivoRdfSupport.addDateTimeValue(model, uri + "-start", employment.getDateFrom()));
        }
        if (Objects.nonNull(employment.getDateTo())) {
            interval.addProperty(model.createProperty(VIVO + "end"),
                VivoRdfSupport.addDateTimeValue(model, uri + "-end", employment.getDateTo()));
        }
    }
}
