package rs.teslaris.exporter.model.converter.push.vivo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

public final class VivoRdfSupport {

    public static final String PROFILE = "vivo";

    public static final String VIVO = "http://vivoweb.org/ontology/core#";

    public static final String OBO = "http://purl.obolibrary.org/obo/";

    public static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    public static final String FOAF = "http://xmlns.com/foaf/0.1/";


    private VivoRdfSupport() {
    }

    public static String organisationUnitUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-ou-" + databaseId;
    }

    public static String personUri(PushTarget target, Integer databaseId) {
        return target.namespace() + "teslaris-person-" + databaseId;
    }

    public static String positionUri(PushTarget target, Integer involvementId) {
        return target.namespace() + "teslaris-position-" + involvementId;
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

    public record Link(String key, String label, String url) {
    }
}
