package rs.teslaris.exporter.model.converter.push.vivo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.ClassPathResource;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportEntityIdentifier;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.SharedNode;
import rs.teslaris.exporter.util.push.SharedNodeKey;

public final class VivoRdfSupport {

    public static final String PROFILE = "vivo";

    public static final String VIVO = "http://vivoweb.org/ontology/core#";

    public static final String OBO = "http://purl.obolibrary.org/obo/";

    public static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    public static final String FOAF = "http://xmlns.com/foaf/0.1/";

    public static final String BIBO = "http://purl.org/ontology/bibo/";

    public static final String EVENT = "http://purl.org/NET/c4dm/event.owl#";

    public static final String FABIO = "http://purl.org/spar/fabio/";

    public static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

    private static final Map<String, String> COUNTRY_URIS = loadCountryUris();

    // Languages that have a messages file, so a code can be translated.
    private static final Set<String> TRANSLATED_LANGUAGES = Set.of("en", "sr");

    private static final List<String> DEFAULT_LANGUAGES = List.of("EN", "SR");


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

    // Journals and book series have their own ID sequence, so they need their own prefix.
    public static String publicationSeriesUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-series-" + databaseId;
    }

    public static String documentUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-document-" + databaseId;
    }

    public static String publisherUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-publisher-" + databaseId;
    }

    // The key comes from SharedNodeKey, so the same name gives the same URI.
    public static String externalPublisherUri(PushTarget target, String key) {
        return target.namespace() + "teslaris-external-publisher-" + key;
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

        var values = contents.stream()
            .filter(content -> Objects.nonNull(content.getContent()) &&
                !content.getContent().isBlank())
            .toList();

        var supported = values.stream().filter(content -> isSupported(content, target)).toList();
        if (!supported.isEmpty()) {
            supported.forEach(content -> subject.addProperty(property, literal(model, content)));
            return;
        }

        // No value in a supported language. Keep the values of one other language, without a
        // language tag, because VIVO shows them in every locale.
        fallbackLanguage(values).ifPresent(language -> values.stream()
            .filter(content -> language.equals(languageKey(content)))
            .forEach(content -> subject.addProperty(property,
                model.createLiteral(content.getContent().trim()))));
    }

    private static boolean isSupported(ExportMultilingualContent content, PushTarget target) {
        var supportedLanguages = target.supportedLanguages();
        return Objects.isNull(supportedLanguages) || supportedLanguages.isEmpty() ||
            (Objects.nonNull(content.getLanguageTag()) && supportedLanguages.stream()
                .anyMatch(language -> language.equalsIgnoreCase(content.getLanguageTag())));
    }

    private static Literal literal(Model model, ExportMultilingualContent content) {
        return Objects.nonNull(content.getLanguageTag()) && !content.getLanguageTag().isBlank() ?
            model.createLiteral(content.getContent().trim(), toBcp47(content.getLanguageTag())) :
            model.createLiteral(content.getContent().trim());
    }

    private static String languageKey(ExportMultilingualContent content) {
        return Objects.isNull(content.getLanguageTag()) ? "" :
            content.getLanguageTag().trim().toUpperCase(Locale.ROOT);
    }

    // The language of the value with the lowest priority number. The content breaks a tie, so
    // the fallback does not change with the order of the snapshot list.
    private static Optional<String> fallbackLanguage(List<ExportMultilingualContent> values) {
        Comparator<ExportMultilingualContent> byPriority =
            Comparator.<ExportMultilingualContent>comparingInt(
                    ExportMultilingualContent::getPriority)
                .thenComparing(VivoRdfSupport::languageKey)
                .thenComparing((ExportMultilingualContent content) -> content.getContent().trim());

        return values.stream().min(byPriority).map(VivoRdfSupport::languageKey);
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

    /**
     * Adds a vivo:DateTimeValue for an ISO date that can be partial ("2024", "2024-05" or
     * "2024-05-12"). The precision follows the given parts. Returns null for a value that is
     * not one of these forms.
     */
    public static Resource addPartialDateTimeValue(Model model, String uri, String isoDate) {
        if (Objects.isNull(isoDate)) {
            return null;
        }

        var value = isoDate.trim();
        String dateTime;
        String precision;
        if (value.matches("\\d{4}")) {
            dateTime = value + "-01-01T00:00:00";
            precision = "yearPrecision";
        } else if (value.matches("\\d{4}-\\d{2}")) {
            dateTime = value + "-01T00:00:00";
            precision = "yearMonthPrecision";
        } else if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            dateTime = value + "T00:00:00";
            precision = "yearMonthDayPrecision";
        } else {
            return null;
        }

        var dateTimeValue = model.createResource(uri);
        dateTimeValue.addProperty(RDF.type, model.createResource(VIVO + "DateTimeValue"));
        dateTimeValue.addProperty(model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral(dateTime, XSDDatatype.XSDdateTime));
        dateTimeValue.addProperty(model.createProperty(VIVO + "dateTimePrecision"),
            model.createResource(VIVO + precision));
        return dateTimeValue;
    }

    private static String keyOf(String code) {
        return code.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    /**
     * Adds a link for each identifier that has a URL template, sorted by code and value. A URL
     * that is already in the list is not added again. Keys are made from the code and are
     * unique in the list.
     */
    public static void addIdentifierLinks(List<Link> links,
                                          List<ExportEntityIdentifier> identifiers) {
        if (Objects.isNull(identifiers)) {
            return;
        }

        var keys = new HashSet<String>();
        links.forEach(link -> keys.add(link.key()));
        identifiers.stream()
            .filter(identifier -> StringUtil.valueExists(identifier.getCode()))
            .sorted(Comparator
                .comparing(ExportEntityIdentifier::getCode)
                .thenComparing(identifier -> String.valueOf(identifier.getValue())))
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
    }

    /**
     * The shared node of an external organisation (only a free-text name), or empty when the
     * name has no text. The node has the type foaf:Organization and the names as labels. The
     * caller writes only the links to the returned URI.
     */
    public static Optional<String> externalOrganisation(PushTarget target,
                                                        List<ExportMultilingualContent> names,
                                                        Map<String, SharedNode> sharedNodes) {
        return SharedNodeKey.of(names).map(key -> {
            var uri = externalOrganisationUnitUri(target, key);
            sharedNodes.computeIfAbsent(uri, nodeUri -> {
                var nodeModel = ModelFactory.createDefaultModel();
                var node = nodeModel.createResource(nodeUri);
                node.addProperty(RDF.type, nodeModel.createResource(FOAF + "Organization"));
                addLiterals(nodeModel, node, RDFS.label, names, target);
                return new SharedNode(nodeUri, nodeModel);
            });
            return uri;
        });
    }

    /**
     * Translations of a message code, as BCP 47 language tag → text, for each supported
     * language that has a messages file (en, sr; Cyrillic is transliterated). Empty when the
     * code has no message.
     */
    public static Map<String, String> translations(MessageSource messageSource,
                                                   PushTarget target, String code) {
        var languages = Objects.nonNull(target.supportedLanguages()) &&
            !target.supportedLanguages().isEmpty() ? target.supportedLanguages() :
            DEFAULT_LANGUAGES;

        var translations = new LinkedHashMap<String, String>();
        languages.forEach(language -> {
            var languageTag = toBcp47(language);
            var locale = Locale.forLanguageTag(languageTag);
            // Without this check, other languages get the text of the system locale.
            if (!TRANSLATED_LANGUAGES.contains(locale.getLanguage())) {
                return;
            }

            try {
                var text = messageSource.getMessage(code, null, locale);
                if (StringUtil.valueExists(text)) {
                    translations.put(languageTag, text.trim());
                }
            } catch (NoSuchMessageException e) {
                // No translation for this code.
            }
        });

        return translations;
    }

    /**
     * A vcard:Individual "{nodeUri}-vcard" for a person who is not in TeslaRIS, as VIVO does
     * for external authors. The display name is "First [Middle] Last", so the last word is the
     * family name.
     */
    public static Resource addExternalPersonVcard(Model model, String nodeUri,
                                                  String displayName) {
        var name = displayName.trim().replaceAll("\\s+", " ");
        var vcard = model.createResource(nodeUri + "-vcard");
        vcard.addProperty(RDF.type, model.createResource(VCARD + "Individual"));
        vcard.addProperty(RDFS.label, name);

        var vcardName = model.createResource(nodeUri + "-vcard-name");
        vcardName.addProperty(RDF.type, model.createResource(VCARD + "Name"));
        vcard.addProperty(model.createProperty(VCARD + "hasName"), vcardName);

        var lastSpace = name.lastIndexOf(' ');
        if (lastSpace > 0) {
            vcardName.addProperty(model.createProperty(VCARD + "givenName"),
                name.substring(0, lastSpace));
            vcardName.addProperty(model.createProperty(VCARD + "familyName"),
                name.substring(lastSpace + 1));
        } else {
            vcardName.addProperty(model.createProperty(VCARD + "familyName"), name);
        }

        return vcard;
    }

    public static String projectUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-project-" + databaseId;
    }

    public static String fundingUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-funding-" + databaseId;
    }

    public static String researchAreaUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-research-area-" + databaseId;
    }

    // The key comes from SharedNodeKey, so the same scheme name gives the same URI.
    public static String conceptSchemeUri(PushTarget target, String key) {
        return target.namespace() + "teslaris-concept-scheme-" + key;
    }

    /**
     * Links the subject to its research areas. Persons and organisation units use
     * vivo:hasResearchArea / vivo:researchAreaOf, other entities vivo:hasSubjectArea /
     * vivo:subjectAreaOf. The subject owns both triples.
     *
     * @param references gets one reference per area, or null when the caller does not count
     *                   links.
     */
    public static void addResearchAreas(Model model, Resource subject, PushTarget target,
                                        List<Integer> researchAreaIds, boolean researchArea,
                                        List<EntityReference> references) {
        if (Objects.isNull(researchAreaIds)) {
            return;
        }

        var property = model.createProperty(VIVO +
            (researchArea ? "hasResearchArea" : "hasSubjectArea"));
        var inverse = model.createProperty(VIVO +
            (researchArea ? "researchAreaOf" : "subjectAreaOf"));

        researchAreaIds.stream().filter(Objects::nonNull).distinct().sorted().forEach(id -> {
            var concept = model.createResource(researchAreaUri(target, id));
            subject.addProperty(property, concept);
            concept.addProperty(inverse, subject);
            if (Objects.nonNull(references)) {
                references.add(new EntityReference(PushEntityType.RESEARCH_AREAS, id));
            }
        });
    }

    public record Link(String key, String label, String url) {
    }
}
