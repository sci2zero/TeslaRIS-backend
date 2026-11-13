package rs.teslaris.exporter.model.converter.push.vivo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.core.io.ClassPathResource;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

public final class VivoRdfSupport {

    public static final String PROFILE = "vivo";

    public static final String VIVO = "http://vivoweb.org/ontology/core#";

    public static final String OBO = "http://purl.obolibrary.org/obo/";

    public static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    public static final String FOAF = "http://xmlns.com/foaf/0.1/";

    public static final String BIBO = "http://purl.org/ontology/bibo/";

    public static final String EVENT = "http://purl.org/NET/c4dm/event.owl#";

    private static final Map<String, String> COUNTRY_URIS = loadCountryUris();


    private VivoRdfSupport() {
    }

    // The country URIs come from the geopolitical data that VIVO installs.
    private static Map<String, String> loadCountryUris() {
        try (var input = new ClassPathResource("export/vivo/vivoCountries.json")
            .getInputStream()) {
            return new ObjectMapper().readValue(input, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load VIVO country URIs.", e);
        }
    }

    // Returns null for a code that VIVO does not have (for example XK).
    public static String countryUri(String isoAlpha2Code) {
        return Objects.nonNull(isoAlpha2Code) ?
            COUNTRY_URIS.get(isoAlpha2Code.trim().toUpperCase(Locale.ROOT)) : null;
    }

    // Links the subject to the VIVO country with "located in" / "location of".
    public static void addCountry(Model model, Resource subject, String isoAlpha2Code) {
        var countryUri = countryUri(isoAlpha2Code);
        if (Objects.isNull(countryUri)) {
            return;
        }

        var country = model.createResource(countryUri);
        subject.addProperty(model.createProperty(OBO + "RO_0001025"), country);
        country.addProperty(model.createProperty(OBO + "RO_0001015"), subject);
    }

    public static String organisationUnitUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-ou-" + databaseId;
    }

    public static String personUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-person-" + databaseId;
    }

    public static String eventUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-event-" + databaseId;
    }

    public static String positionUri(PushTarget target, Integer involvementId) {
        return target.namespace() + "teslaris-position-" + involvementId;
    }

    // The key comes from SharedNodeKey, so the same name gives the same URI.
    public static String externalOrganisationUnitUri(PushTarget target, String key) {
        return target.namespace() + "teslaris-external-org-" + key;
    }

    public static String toBcp47(String languageTag) {
        return switch (languageTag.toUpperCase(Locale.ROOT)) {
            case "SR-CYR" -> "sr-Cyrl";
            case "SR-LAT" -> "sr-Latn";
            default -> languageTag.toLowerCase(Locale.ROOT);
        };
    }

    public static void addLiterals(Model model, Resource subject, Property property,
                                   List<ExportMultilingualContent> contents,
                                   PushTarget target) {
        if (Objects.isNull(contents)) {
            return;
        }

        var supportedLanguages = target.supportedLanguages();
        contents.stream()
            .filter(content -> Objects.nonNull(content.getContent()) &&
                !content.getContent().isBlank())
            .filter(content -> Objects.isNull(supportedLanguages) ||
                supportedLanguages.isEmpty() || (Objects.nonNull(content.getLanguageTag()) &&
                supportedLanguages.stream()
                    .anyMatch(language -> language.equalsIgnoreCase(content.getLanguageTag()))))
            .forEach(content -> subject.addProperty(property,
                Objects.nonNull(content.getLanguageTag()) && !content.getLanguageTag().isBlank() ?
                    model.createLiteral(content.getContent().trim(),
                        toBcp47(content.getLanguageTag())) :
                    model.createLiteral(content.getContent().trim())));
    }

    // Keywords are stored one per line. Each keyword becomes one literal.
    public static void addKeywords(Model model, Resource subject,
                                   List<ExportMultilingualContent> keywords, PushTarget target) {
        if (Objects.isNull(keywords)) {
            return;
        }

        var split = new ArrayList<ExportMultilingualContent>();
        keywords.stream().filter(content -> Objects.nonNull(content.getContent()))
            .forEach(content -> content.getContent().lines()
                .map(String::trim).filter(keyword -> !keyword.isEmpty())
                .forEach(keyword -> split.add(new ExportMultilingualContent(
                    content.getLanguageTag(), keyword, content.getPriority()))));

        addLiterals(model, subject, model.createProperty(VIVO + "freetextKeyword"), split,
            target);
    }

    public static void addIdentifierLink(List<Link> links, String key, String label,
                                         String urlPrefix, String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return;
        }

        var trimmed = value.trim();
        links.add(new Link(key, label, trimmed.startsWith("http") ? trimmed : urlPrefix + trimmed));
    }

    public static List<Link> withWebpages(List<Link> fixedLinks, List<String> uris) {
        var links = new ArrayList<>(fixedLinks);
        var fixedUrls = fixedLinks.stream().map(Link::url).toList();

        // Sort the URIs, because the snapshot does not keep their order.
        if (Objects.nonNull(uris)) {
            var webpages = uris.stream().filter(webpage -> Objects.nonNull(webpage) &&
                    !webpage.isBlank()).map(String::trim).distinct()
                .filter(webpage -> !fixedUrls.contains(webpage)).sorted().toList();
            for (int i = 0; i < webpages.size(); i++) {
                links.add(new Link(String.valueOf(i + 1), null, webpages.get(i)));
            }
        }

        return links;
    }

    public static Resource addVcard(Model model, Resource owner, String ownerUri,
                                    String vcardType) {
        var vcard = model.createResource(ownerUri + "-vcard");
        vcard.addProperty(RDF.type, model.createResource(VCARD + vcardType));
        owner.addProperty(model.createProperty(OBO + "ARG_2000028"), vcard);
        vcard.addProperty(model.createProperty(OBO + "ARG_2000029"), owner);
        return vcard;
    }

    public static void addUrls(Model model, Resource vcard, String ownerUri, List<Link> links) {
        for (int i = 0; i < links.size(); i++) {
            var link = links.get(i);
            var url = model.createResource(ownerUri + "-url-" + link.key());
            url.addProperty(RDF.type, model.createResource(VCARD + "URL"));
            url.addProperty(model.createProperty(VCARD + "url"),
                model.createTypedLiteral(link.url(), XSDDatatype.XSDanyURI));
            url.addLiteral(model.createProperty(VIVO + "rank"),
                model.createTypedLiteral(i + 1, XSDDatatype.XSDint));
            if (Objects.nonNull(link.label())) {
                url.addProperty(RDFS.label, link.label());
            }
            vcard.addProperty(model.createProperty(VCARD + "hasURL"), url);
        }
    }

    public static Resource addDateTimeValue(Model model, String uri, LocalDate date) {
        var value = model.createResource(uri);
        value.addProperty(RDF.type, model.createResource(VIVO + "DateTimeValue"));
        value.addProperty(model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral(date + "T00:00:00", XSDDatatype.XSDdateTime));
        value.addProperty(model.createProperty(VIVO + "dateTimePrecision"),
            model.createResource(VIVO + "yearMonthDayPrecision"));
        return value;
    }

    // Adds a vivo:DateTimeInterval node "{uri}-interval" with start and end values, day
    // precision. Adds nothing when both dates are null.
    public static void addDateTimeInterval(Model model, Resource subject, String uri,
                                           LocalDate start, LocalDate end) {
        if (Objects.isNull(start) && Objects.isNull(end)) {
            return;
        }

        var interval = model.createResource(uri + "-interval");
        interval.addProperty(RDF.type, model.createResource(VIVO + "DateTimeInterval"));
        subject.addProperty(model.createProperty(VIVO + "dateTimeInterval"), interval);

        if (Objects.nonNull(start)) {
            interval.addProperty(model.createProperty(VIVO + "start"),
                addDateTimeValue(model, uri + "-start", start));
        }
        if (Objects.nonNull(end)) {
            interval.addProperty(model.createProperty(VIVO + "end"),
                addDateTimeValue(model, uri + "-end", end));
        }
    }

    public record Link(String key, String label, String url) {
    }
}
