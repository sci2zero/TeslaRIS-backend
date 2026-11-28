package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.BIBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.FABIO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.common.ExportPublisher;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapping;
import rs.teslaris.exporter.util.push.SharedNode;
import rs.teslaris.exporter.util.push.SharedNodeKey;

/**
 * Maps journals, book series and documents. A document writes only the links to its parents
 * (journal, proceedings, monograph, event, persons, publisher), each with its inverse, and
 * never their type or label. The parents get their own triples when they are pushed.
 */
@Component
@RequiredArgsConstructor
public class VivoDocumentMapper implements RdfMapper<ExportDocument> {

    // "information content entity", for types that have no VIVO class.
    private static final String INFORMATION_CONTENT_ENTITY = OBO + "IAO_0000030";

    // Classes used for outputs that are not under obo:IAO_0000030 in the VIVO ontology.
    private static final Set<String> NOT_INFORMATION_CONTENT_ENTITIES =
        Set.of(OBO + "ERO_0000071", BIBO + "Performance", VIVO + "Exhibit",
            VIVO + "Presentation", VIVO + "InvitedTalk", BIBO + "Interview");

    private final VivoTeslarisLinks teslarisLinks;


    public static boolean isPublicationSeries(ExportPublicationType type) {
        return type == ExportPublicationType.JOURNAL || type == ExportPublicationType.BOOK_SERIES;
    }

    public static String uri(PushTarget target, ExportDocument snapshot) {
        return isPublicationSeries(snapshot.getType()) ?
            VivoRdfSupport.publicationSeriesUri(target, snapshot.getDatabaseId()) :
            VivoRdfSupport.documentUri(target, snapshot.getDatabaseId());
    }

    /**
     * Only classes that are in the VIVO 1.15 ontology. VIVO has no subtype property, so
     * concrete types that map to the same class cannot be told apart in VIVO.
     */
    static List<String> vivoClasses(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getType())) {
            return List.of(BIBO + "Document");
        }

        return switch (snapshot.getType()) {
            case JOURNAL -> List.of(BIBO + "Journal");
            case BOOK_SERIES -> List.of(BIBO + "Series");
            case PROCEEDINGS -> List.of(BIBO + "Proceedings");
            case THESIS -> List.of(BIBO + "Thesis");
            case INTELLECTUAL_PROPERTY -> List.of(intellectualPropertyClass(snapshot));
            case INTANGIBLE_PRODUCT -> asOutput(intangibleProductClass(snapshot));
            case PERFORMANCE_RELATED_OUTPUT -> asOutput(performanceRelatedOutputClass(snapshot));
            // VIVO has no class for these, so they are information content entities.
            case MATERIAL_PRODUCT, GENETIC_MATERIAL -> List.of(INFORMATION_CONTENT_ENTITY);
            case JOURNAL_PUBLICATION -> List.of(journalPublicationClass(snapshot));
            case PROCEEDINGS_PUBLICATION -> List.of(proceedingsPublicationClass(snapshot));
            case MONOGRAPH -> monographClasses(snapshot);
            case MONOGRAPH_PUBLICATION -> List.of(monographPublicationClass(snapshot));
            default -> List.of(BIBO + "Document");
        };
    }

    /**
     * VIVO shows the authors (faux property on vivo:relatedBy → vivo:Authorship) only for
     * subclasses of obo:IAO_0000030. Software (obo:ERO_0000071) is a material entity, and
     * bibo:Performance, vivo:Exhibit, vivo:Presentation, vivo:InvitedTalk and bibo:Interview are
     * events, so these also get obo:IAO_0000030.
     */
    private static List<String> asOutput(String vivoClass) {
        return NOT_INFORMATION_CONTENT_ENTITIES.contains(vivoClass) ?
            List.of(vivoClass, INFORMATION_CONTENT_ENTITY) : List.of(vivoClass);
    }

    private static String intellectualPropertyClass(ExportDocument snapshot) {
        return snapshot.getIntellectualPropertyType() ==
            IntellectualPropertyType.PATENT ?
            BIBO + "Patent" : INFORMATION_CONTENT_ENTITY;
    }

    private static String intangibleProductClass(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getIntangibleProductType())) {
            return INFORMATION_CONTENT_ENTITY;
        }

        return switch (snapshot.getIntangibleProductType()) {
            case SOFTWARE -> OBO + "ERO_0000071";
            case DATASET -> VIVO + "Dataset";
            case STANDARD -> BIBO + "Standard";
            case WEBSITE, ONLINE_RESOURCE -> BIBO + "Webpage";
            case VIDEO_RECORDING, RADIO_TV_PROGRAM -> VIVO + "Video";
            case AUDIO_RECORDING -> BIBO + "AudioDocument";
            default -> INFORMATION_CONTENT_ENTITY;
        };
    }

    private static String performanceRelatedOutputClass(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getPerformanceRelatedOutputType())) {
            return INFORMATION_CONTENT_ENTITY;
        }

        return switch (snapshot.getPerformanceRelatedOutputType()) {
            case SLIDES -> BIBO + "Slideshow";
            case POSTER -> VIVO + "ConferencePoster";
            case CATALOGUE -> VIVO + "Catalog";
            case ARTISTIC_EXHIBITION, CURATORIAL_MUSEUM_EXHIBITION -> VIVO + "Exhibit";
            case MUSICAL_PERFORMANCE, ART_PERFORMANCE, THEATRIC_PLAY -> BIBO + "Performance";
            case NON_RESEARCH_PRESENTATION -> Boolean.TRUE.equals(snapshot.getInvitedTalk()) ?
                VIVO + "InvitedTalk" : VIVO + "Presentation";
            case BROADCAST_INTERVIEW, TEXT_INTERVIEW -> BIBO + "Interview";
            default -> INFORMATION_CONTENT_ENTITY;
        };
    }

    private static String journalPublicationClass(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getJournalPublicationType())) {
            return BIBO + "Article";
        }

        return switch (snapshot.getJournalPublicationType()) {
            case RESEARCH_ARTICLE, REVIEW_ARTICLE -> BIBO + "AcademicArticle";
            case EDITORIAL -> VIVO + "EditorialArticle";
            case BOOK_REVIEW, SCIENTIFIC_CRITIC -> VIVO + "Review";
            case CORRECTION -> FABIO + "Erratum";
            case COMMENT, POLEMICS -> FABIO + "Comment";
            case TRANSLATION -> VIVO + "Translation";
            default -> BIBO + "Article";
        };
    }

    private static String proceedingsPublicationClass(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getProceedingsPublicationType())) {
            return BIBO + "Article";
        }

        return switch (snapshot.getProceedingsPublicationType()) {
            case REGULAR_FULL_ARTICLE, INVITED_FULL_ARTICLE -> VIVO + "ConferencePaper";
            case REGULAR_ABSTRACT_ARTICLE, INVITED_ABSTRACT_ARTICLE -> VIVO + "Abstract";
            default -> BIBO + "Article";
        };
    }

    private static List<String> monographClasses(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getMonographType())) {
            return List.of(BIBO + "Book");
        }

        return switch (snapshot.getMonographType()) {
            case EDITED_BOOK -> List.of(BIBO + "EditedBook");
            case TRANSLATION -> List.of(BIBO + "Book", VIVO + "Translation");
            case MANUAL -> List.of(BIBO + "Manual");
            case REPORT -> List.of(BIBO + "Report");
            case WORKING_PAPER -> List.of(VIVO + "WorkingPaper");
            case JOURNAL_ISSUE -> List.of(BIBO + "Issue");
            case PROCEEDINGS -> List.of(BIBO + "Proceedings");
            case PREPRINT, DATA_MANAGEMENT_PLAN -> List.of(BIBO + "Document");
            default -> List.of(BIBO + "Book");
        };
    }

    private static String monographPublicationClass(ExportDocument snapshot) {
        if (Objects.isNull(snapshot.getMonographPublicationType())) {
            return BIBO + "BookSection";
        }

        return switch (snapshot.getMonographPublicationType()) {
            case CHAPTER -> BIBO + "Chapter";
            case BOOK_REVIEW -> VIVO + "Review";
            default -> BIBO + "BookSection";
        };
    }

    // The frontend landing page. Book series pages need a login, so they get no link.
    private static String frontendPath(ExportPublicationType type) {
        if (Objects.isNull(type)) {
            return null;
        }

        return switch (type) {
            case JOURNAL -> "journals";
            case PROCEEDINGS -> "proceedings";
            case JOURNAL_PUBLICATION -> "scientific-results/journal-publication";
            case PROCEEDINGS_PUBLICATION -> "scientific-results/proceedings-publication";
            case MONOGRAPH -> "scientific-results/monograph";
            case MONOGRAPH_PUBLICATION -> "scientific-results/monograph-publication";
            case THESIS -> "scientific-results/thesis";
            case INTELLECTUAL_PROPERTY -> "scientific-results/intellectual-property";
            case INTANGIBLE_PRODUCT -> "scientific-results/intangible-product";
            case MATERIAL_PRODUCT -> "scientific-results/material-product";
            case GENETIC_MATERIAL -> "scientific-results/genetic-material";
            case PERFORMANCE_RELATED_OUTPUT -> "scientific-results/performance-related-output";
            default -> null;
        };
    }

    private static boolean hasText(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    // "Title: Subtitle" when the subtitle has the same language as the title.
    static List<ExportMultilingualContent> labels(ExportDocument snapshot) {
        var labels = new ArrayList<ExportMultilingualContent>();
        if (Objects.isNull(snapshot.getTitle())) {
            return labels;
        }

        var subtitles = Objects.nonNull(snapshot.getSubtitle()) ? snapshot.getSubtitle() :
            List.<ExportMultilingualContent>of();
        snapshot.getTitle().stream().filter(title -> hasText(title.getContent()))
            .forEach(title -> {
                var subtitle = subtitles.stream()
                    .filter(candidate -> hasText(candidate.getContent()) &&
                        Objects.equals(candidate.getLanguageTag(), title.getLanguageTag()))
                    .findFirst();
                var label = subtitle.map(value -> title.getContent().trim() + ": " +
                    value.getContent().trim()).orElse(title.getContent().trim());
                labels.add(new ExportMultilingualContent(title.getLanguageTag(), label,
                    title.getPriority()));
            });

        return labels;
    }

    private static String bareDoi(String doi) {
        return doi.trim().replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "")
            .replaceFirst("(?i)^doi:", "");
    }

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportDocument> sourceType() {
        return ExportDocument.class;
    }

    @Override
    public Model toRdf(ExportDocument snapshot, PushTarget target) {
        return map(snapshot, target).model();
    }

    @Override
    public RdfMapping map(ExportDocument snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var sharedNodes = new LinkedHashMap<String, SharedNode>();
        var references = new ArrayList<EntityReference>();

        var uri = uri(target, snapshot);
        var document = model.createResource(uri);

        vivoClasses(snapshot).forEach(
            vivoClass -> document.addProperty(RDF.type, model.createResource(vivoClass)));
        VivoRdfSupport.addLiterals(model, document, RDFS.label, labels(snapshot), target);
        VivoRdfSupport.addLiterals(model, document, model.createProperty(VIVO + "abbreviation"),
            snapshot.getNameAbbreviation(), target);

        if (isPublicationSeries(snapshot.getType())) {
            addLiteral(document, model.createProperty(BIBO + "issn"), snapshot.getPrintIssn());
            addLiteral(document, model.createProperty(BIBO + "eissn"), snapshot.getEIssn());
        } else {
            addDocumentData(model, document, uri, snapshot, target);
            addVenue(model, document, target, snapshot, references);
            addEvent(model, document, target, snapshot, references);
            addPublishers(model, document, target, snapshot, sharedNodes);
            addContributions(model, document, uri, target, snapshot.getAuthors(), "Authorship",
                "authorship", references);
            addContributions(model, document, uri, target, snapshot.getEditors(), "Editorship",
                "editorship", references);
            addThesisGrantor(model, document, target, snapshot, references);
        }

        VivoRdfSupport.addResearchAreas(model, document, target, snapshot.getResearchAreaIds(),
            false, references);

        var links = VivoRdfSupport.withWebpages(links(snapshot), snapshot.getUris());
        if (!links.isEmpty()) {
            var vcard = VivoRdfSupport.addVcard(model, document, uri, "Kind");
            VivoRdfSupport.addUrls(model, vcard, uri, links);
        }

        return new RdfMapping(model, List.copyOf(sharedNodes.values()), List.copyOf(references));
    }

    private void addLiteral(Resource subject, Property property, String value) {
        if (hasText(value)) {
            subject.addProperty(property, value.trim());
        }
    }

    private void addDocumentData(Model model, Resource document, String uri,
                                 ExportDocument snapshot, PushTarget target) {
        VivoRdfSupport.addLiterals(model, document, model.createProperty(BIBO + "abstract"),
            snapshot.getDescription(), target);
        VivoRdfSupport.addKeywords(model, document, snapshot.getKeywords(), target);

        var date = VivoRdfSupport.addPartialDateTimeValue(model, uri + "-date",
            snapshot.getDocumentDate());
        if (Objects.isNull(date) && Objects.nonNull(snapshot.getThesisDefenceDate())) {
            date = VivoRdfSupport.addDateTimeValue(model, uri + "-date",
                snapshot.getThesisDefenceDate());
        }
        if (Objects.nonNull(date)) {
            document.addProperty(model.createProperty(VIVO + "dateTimeValue"), date);
        }

        addLiteral(document, model.createProperty(BIBO + "volume"), snapshot.getVolume());
        addLiteral(document, model.createProperty(BIBO + "issue"), snapshot.getIssue());
        addLiteral(document, model.createProperty(BIBO + "pageStart"), snapshot.getStartPage());
        addLiteral(document, model.createProperty(BIBO + "pageEnd"), snapshot.getEndPage());
        addNumber(model, document, snapshot);

        if (hasText(snapshot.getDoi())) {
            document.addProperty(model.createProperty(BIBO + "doi"), bareDoi(snapshot.getDoi()));
        }

        addIsbn(model, document, snapshot.getPrintIsbn());
        addIsbn(model, document, snapshot.getEIsbn());
    }

    // For products the number is an internal number, so it is not sent. A patent number has
    // its own VIVO property.
    private void addNumber(Model model, Resource document, ExportDocument snapshot) {
        var type = snapshot.getType();
        if (type == ExportPublicationType.INTANGIBLE_PRODUCT ||
            type == ExportPublicationType.MATERIAL_PRODUCT ||
            type == ExportPublicationType.GENETIC_MATERIAL) {
            return;
        }

        var property = type == ExportPublicationType.INTELLECTUAL_PROPERTY &&
            snapshot.getIntellectualPropertyType() ==
                IntellectualPropertyType.PATENT ?
            VIVO + "patentNumber" : BIBO + "number";
        addLiteral(document, model.createProperty(property), snapshot.getNumber());
    }

    // VIVO has separate properties for 10- and 13-digit ISBNs.
    private void addIsbn(Model model, Resource document, String isbn) {
        if (!hasText(isbn)) {
            return;
        }

        var digits = isbn.replaceAll("[^0-9Xx]", "");
        var property = digits.length() == 10 ? "isbn10" : "isbn13";
        document.addProperty(model.createProperty(BIBO + property), isbn.trim());
    }

    private void addVenue(Model model, Resource document, PushTarget target,
                          ExportDocument snapshot, List<EntityReference> references) {
        if (Objects.isNull(snapshot.getType())) {
            return;
        }

        String venueUri = null;
        switch (snapshot.getType()) {
            case JOURNAL_PUBLICATION, PROCEEDINGS, MONOGRAPH -> {
                var series = snapshot.getJournal();
                if (Objects.nonNull(series) && Objects.nonNull(series.getDatabaseId())) {
                    venueUri = VivoRdfSupport.publicationSeriesUri(target,
                        series.getDatabaseId());
                    references.add(new EntityReference(PushEntityType.PUBLICATION_SERIES,
                        series.getDatabaseId()));
                }
            }
            case PROCEEDINGS_PUBLICATION -> venueUri =
                documentVenue(target, snapshot.getProceedings(), references);
            case MONOGRAPH_PUBLICATION -> venueUri =
                documentVenue(target, snapshot.getMonograph(), references);
            default -> {
            }
        }

        if (Objects.isNull(venueUri)) {
            return;
        }

        var venue = model.createResource(venueUri);
        document.addProperty(model.createProperty(VIVO + "hasPublicationVenue"), venue);
        venue.addProperty(model.createProperty(VIVO + "publicationVenueFor"), document);
    }

    private String documentVenue(PushTarget target, ExportDocument parent,
                                 List<EntityReference> references) {
        if (Objects.isNull(parent) || Objects.isNull(parent.getDatabaseId())) {
            return null;
        }

        references.add(new EntityReference(PushEntityType.DOCUMENTS, parent.getDatabaseId()));
        return VivoRdfSupport.documentUri(target, parent.getDatabaseId());
    }

    private void addEvent(Model model, Resource document, PushTarget target,
                          ExportDocument snapshot, List<EntityReference> references) {
        var event = snapshot.getEvent();
        if (Objects.isNull(event) || Objects.isNull(event.getDatabaseId())) {
            return;
        }

        var eventResource =
            model.createResource(VivoRdfSupport.eventUri(target, event.getDatabaseId()));
        document.addProperty(model.createProperty(BIBO + "presentedAt"), eventResource);
        eventResource.addProperty(model.createProperty(BIBO + "presents"), document);
        references.add(new EntityReference(PushEntityType.EVENTS, event.getDatabaseId()));
    }

    // A publisher entity gives a node from its ID. A publisher with only a name (old snapshot
    // or displayPublisher) gives a node from the name. Both are shared by many documents.
    private void addPublishers(Model model, Resource document, PushTarget target,
                               ExportDocument snapshot, Map<String, SharedNode> sharedNodes) {
        var publishers = Objects.nonNull(snapshot.getPublishers()) ? snapshot.getPublishers() :
            List.<ExportPublisher>of();

        publishers.forEach(publisher -> {
            if (Objects.nonNull(publisher.getDatabaseId())) {
                addPublisher(model, document, target, sharedNodes,
                    VivoRdfSupport.publisherUri(target, publisher.getDatabaseId()),
                    publisher.getName(), true);
            } else {
                SharedNodeKey.of(publisher.getName()).ifPresent(key ->
                    addPublisher(model, document, target, sharedNodes,
                        VivoRdfSupport.externalPublisherUri(target, key), publisher.getName(),
                        false));
            }
        });

        if (publishers.isEmpty()) {
            SharedNodeKey.of(snapshot.getDisplayPublisher()).ifPresent(key ->
                addPublisher(model, document, target, sharedNodes,
                    VivoRdfSupport.externalPublisherUri(target, key),
                    snapshot.getDisplayPublisher(), false));
        }
    }

    private void addPublisher(Model model, Resource document, PushTarget target,
                              Map<String, SharedNode> sharedNodes, String publisherUri,
                              List<ExportMultilingualContent> name,
                              boolean replaceWhenChanged) {
        sharedNodes.computeIfAbsent(publisherUri, nodeUri -> {
            var nodeModel = ModelFactory.createDefaultModel();
            var node = nodeModel.createResource(nodeUri);
            node.addProperty(RDF.type, nodeModel.createResource(VIVO + "Publisher"));
            VivoRdfSupport.addLiterals(nodeModel, node, RDFS.label, name, target);
            return new SharedNode(nodeUri, nodeModel, replaceWhenChanged);
        });

        var publisher = model.createResource(publisherUri);
        document.addProperty(model.createProperty(VIVO + "publisher"), publisher);
        publisher.addProperty(model.createProperty(VIVO + "publisherOf"), document);
    }

    /**
     * One vivo:Authorship or vivo:Editorship node per contribution, with vivo:rank. The node
     * URI uses the position in the list, which is sorted by the order number. A contribution
     * without a TeslaRIS person links to a vcard:Individual with the display name, as VIVO does
     * for external authors.
     */
    private void addContributions(Model model, Resource document, String uri, PushTarget target,
                                  List<ExportContribution> contributions, String vivoClass,
                                  String uriPart, List<EntityReference> references) {
        if (Objects.isNull(contributions)) {
            return;
        }

        var relates = model.createProperty(VIVO + "relates");
        var relatedBy = model.createProperty(VIVO + "relatedBy");
        var sorted = contributions.stream().filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(ExportContribution::getOrderNumber)).toList();

        for (int i = 0; i < sorted.size(); i++) {
            var contribution = sorted.get(i);
            var nodeUri = uri + "-" + uriPart + "-" + (i + 1);

            Resource agent;
            var person = contribution.getPerson();
            if (VivoRdfSupport.isPushed(person)) {
                agent = model.createResource(
                    VivoRdfSupport.personUri(target, person.getDatabaseId()));
                references.add(
                    new EntityReference(PushEntityType.PERSONS, person.getDatabaseId()));
            } else if (hasText(contribution.getDisplayName())) {
                agent = VivoRdfSupport.addExternalPersonVcard(model, nodeUri,
                    contribution.getDisplayName());
            } else {
                continue;
            }

            var node = model.createResource(nodeUri);
            node.addProperty(RDF.type, model.createResource(VIVO + vivoClass));
            node.addLiteral(model.createProperty(VIVO + "rank"),
                model.createTypedLiteral(i + 1, XSDDatatype.XSDint));
            node.addProperty(relates, document);
            node.addProperty(relates, agent);
            document.addProperty(relatedBy, node);
            agent.addProperty(relatedBy, node);
        }
    }

    private void addThesisGrantor(Model model, Resource document, PushTarget target,
                                  ExportDocument snapshot, List<EntityReference> references) {
        var grantor = snapshot.getThesisGrantor();
        if (snapshot.getType() != ExportPublicationType.THESIS || Objects.isNull(grantor) ||
            Objects.isNull(grantor.getDatabaseId())) {
            return;
        }

        document.addProperty(model.createProperty(BIBO + "issuer"), model.createResource(
            VivoRdfSupport.organisationUnitUri(target, grantor.getDatabaseId())));
        references.add(
            new EntityReference(PushEntityType.ORGANISATION_UNITS, grantor.getDatabaseId()));
    }

    private List<Link> links(ExportDocument snapshot) {
        var links = new ArrayList<Link>();

        var path = frontendPath(snapshot.getType());
        if (Objects.nonNull(path)) {
            links.add(teslarisLinks.profileLink(path, snapshot.getDatabaseId()));
        }

        VivoRdfSupport.addIdentifierLink(links, "scopus", "Scopus",
            "https://www.scopus.com/pages/publications/", snapshot.getScopus());
        VivoRdfSupport.addIdentifierLink(links, "openalex", "OpenAlex", "https://openalex.org/",
            snapshot.getOpenAlex());
        VivoRdfSupport.addIdentifierLink(links, "wos", "Web of Science",
            "https://www.webofscience.com/wos/woscc/full-record/WOS:",
            snapshot.getWebOfScience());
        return links;
    }
}
