package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import rs.teslaris.core.model.document.GeneticMaterialType;
import rs.teslaris.core.model.document.IntangibleProductType;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.MaterialProductType;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.common.ExportPublisher;
import rs.teslaris.exporter.model.converter.push.vivo.VivoDocumentMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoDocumentMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String BIBO = "http://purl.org/ontology/bibo/";

    private static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private static final String ARTICLE = NS + "teslaris-document-20";

    private final VivoDocumentMapper mapper = new VivoDocumentMapper(
        new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"));


    private static PushTarget target() {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null,
            List.of("EN", "SR"), List.of("documents"), null);
    }

    private static List<ExportMultilingualContent> english(String content) {
        return new ArrayList<>(List.of(new ExportMultilingualContent("EN", content, 1)));
    }

    private static ExportDocument document(ExportPublicationType type, Integer id) {
        var document = new ExportDocument();
        document.setType(type);
        document.setDatabaseId(id);
        document.setTitle(english("Title " + id));
        return document;
    }

    private static ExportContribution contribution(Integer personId, String displayName,
                                                   int orderNumber) {
        var contribution = new ExportContribution();
        contribution.setDisplayName(displayName);
        contribution.setOrderNumber(orderNumber);
        if (personId != null) {
            var person = new ExportPerson();
            person.setDatabaseId(personId);
            contribution.setPerson(person);
        }
        return contribution;
    }

    private static ExportDocument article() {
        var article = document(ExportPublicationType.JOURNAL_PUBLICATION, 20);
        article.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        article.setTitle(new ArrayList<>(List.of(
            new ExportMultilingualContent("EN", "Testing RDF", 1),
            new ExportMultilingualContent("SR", "Testiranje RDF-a", 2))));
        article.setSubtitle(english("A case study"));
        article.setDescription(english("An abstract."));
        article.setKeywords(english("rdf\nvivo"));
        article.setDocumentDate("2024-05");
        article.setVolume("12");
        article.setIssue("3");
        article.setStartPage("10");
        article.setEndPage("20");
        article.setDoi("https://doi.org/10.1000/xyz");
        article.setScopus("85000000000");
        article.setJournal(document(ExportPublicationType.JOURNAL, 20));
        article.setAuthors(new ArrayList<>(List.of(
            contribution(null, "Ana  Maria Petrovic", 2),
            contribution(7, "Ivan Mrsulja", 1),
            contribution(null, " ", 3))));
        article.setEditors(new ArrayList<>(List.of(contribution(8, "Editor Person", 1))));
        return article;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
    }

    @Test
    public void shouldMapArticleData() {
        // When
        var model = mapper.toRdf(article(), target());

        // Then
        var article = model.createResource(ARTICLE);
        assertTrue(has(model, ARTICLE, RDF.type.getURI(), BIBO + "AcademicArticle"));
        assertTrue(model.contains(article, RDFS.label,
            model.createLiteral("Testing RDF: A case study", "en")));
        assertTrue(model.contains(article, RDFS.label,
            model.createLiteral("Testiranje RDF-a", "sr")));
        assertTrue(model.contains(article, model.createProperty(BIBO + "abstract"),
            model.createLiteral("An abstract.", "en")));
        assertEquals(2, model.listObjectsOfProperty(article,
            model.createProperty(VIVO + "freetextKeyword")).toList().size());
        assertTrue(model.contains(article, model.createProperty(BIBO + "volume"), "12"));
        assertTrue(model.contains(article, model.createProperty(BIBO + "issue"), "3"));
        assertTrue(model.contains(article, model.createProperty(BIBO + "pageStart"), "10"));
        assertTrue(model.contains(article, model.createProperty(BIBO + "pageEnd"), "20"));
        assertTrue(model.contains(article, model.createProperty(BIBO + "doi"), "10.1000/xyz"));

        assertTrue(has(model, ARTICLE, VIVO + "dateTimeValue", ARTICLE + "-date"));
        assertTrue(model.contains(model.createResource(ARTICLE + "-date"),
            model.createProperty(VIVO + "dateTime"),
            model.createTypedLiteral("2024-05-01T00:00:00", XSDDatatype.XSDdateTime)));
        assertTrue(has(model, ARTICLE + "-date", VIVO + "dateTimePrecision",
            VIVO + "yearMonthPrecision"));

        assertTrue(model.contains(model.createResource(ARTICLE + "-url-teslaris"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral(
                "http://localhost:5173/sr/scientific-results/journal-publication/20",
                XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(model.createResource(ARTICLE + "-url-scopus"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://www.scopus.com/pages/publications/85000000000",
                XSDDatatype.XSDanyURI)));
    }

    @Test
    public void shouldLinkJournalWithoutWritingItsTypeOrLabel() {
        // When
        var mapping = mapper.map(article(), target());

        // Then
        var model = mapping.model();
        var journal = NS + "teslaris-series-20";
        assertTrue(has(model, ARTICLE, VIVO + "hasPublicationVenue", journal));
        assertTrue(has(model, journal, VIVO + "publicationVenueFor", ARTICLE));
        assertFalse(model.contains(model.createResource(journal), RDF.type));
        assertFalse(model.contains(model.createResource(journal), RDFS.label));
        assertTrue(mapping.references().contains(
            new EntityReference(PushEntityType.PUBLICATION_SERIES, 20)));
    }

    @Test
    public void shouldMapAuthorsInOrderWithExternalAuthorAsVcard() {
        // When
        var mapping = mapper.map(article(), target());

        // Then
        var model = mapping.model();
        var first = ARTICLE + "-authorship-1";
        var second = ARTICLE + "-authorship-2";
        var person = NS + "teslaris-person-7";

        assertTrue(has(model, first, RDF.type.getURI(), VIVO + "Authorship"));
        assertTrue(model.contains(model.createResource(first),
            model.createProperty(VIVO + "rank"), model.createTypedLiteral(1, XSDDatatype.XSDint)));
        assertTrue(has(model, first, VIVO + "relates", person));
        assertTrue(has(model, person, VIVO + "relatedBy", first));
        assertTrue(has(model, ARTICLE, VIVO + "relatedBy", first));
        assertTrue(mapping.references().contains(new EntityReference(PushEntityType.PERSONS, 7)));

        var vcard = second + "-vcard";
        assertTrue(has(model, second, VIVO + "relates", vcard));
        assertTrue(has(model, vcard, RDF.type.getURI(), VCARD + "Individual"));
        assertTrue(model.contains(model.createResource(vcard), RDFS.label,
            "Ana Maria Petrovic"));
        assertTrue(model.contains(model.createResource(vcard + "-name"),
            model.createProperty(VCARD + "familyName"), "Petrovic"));
        assertTrue(model.contains(model.createResource(vcard + "-name"),
            model.createProperty(VCARD + "givenName"), "Ana Maria"));

        // A contribution without a person and without a name is not sent.
        assertFalse(model.containsResource(model.createResource(ARTICLE + "-authorship-3")));

        assertTrue(has(model, ARTICLE + "-editorship-1", RDF.type.getURI(),
            VIVO + "Editorship"));
        assertTrue(has(model, ARTICLE + "-editorship-1", VIVO + "relates",
            NS + "teslaris-person-8"));

        // The person's own data is not written by the document.
        assertFalse(model.contains(model.createResource(person), RDF.type));
    }

    @Test
    public void shouldMapJournalAsSeriesWithIssn() {
        // Given
        var journal = document(ExportPublicationType.JOURNAL, 20);
        journal.setPrintIssn("1234-5678");
        journal.setEIssn("8765-4321");

        // When
        var model = mapper.toRdf(journal, target());

        // Then
        var uri = NS + "teslaris-series-20";
        assertTrue(has(model, uri, RDF.type.getURI(), BIBO + "Journal"));
        assertTrue(model.contains(model.createResource(uri), model.createProperty(BIBO + "issn"),
            "1234-5678"));
        assertTrue(model.contains(model.createResource(uri), model.createProperty(BIBO + "eissn"),
            "8765-4321"));
        assertTrue(model.contains(model.createResource(uri + "-url-teslaris"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("http://localhost:5173/sr/journals/20",
                XSDDatatype.XSDanyURI)));
    }

    @Test
    public void shouldLinkPaperToProceedingsAndEvent() {
        // Given
        var paper = document(ExportPublicationType.PROCEEDINGS_PUBLICATION, 30);
        paper.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        paper.setProceedings(document(ExportPublicationType.PROCEEDINGS, 31));
        var event = new ExportEvent();
        event.setDatabaseId(5);
        paper.setEvent(event);

        // When
        var mapping = mapper.map(paper, target());

        // Then
        var model = mapping.model();
        var uri = NS + "teslaris-document-30";
        assertTrue(has(model, uri, RDF.type.getURI(), VIVO + "ConferencePaper"));
        assertTrue(has(model, uri, VIVO + "hasPublicationVenue", NS + "teslaris-document-31"));
        assertTrue(has(model, uri, BIBO + "presentedAt", NS + "teslaris-event-5"));
        assertTrue(has(model, NS + "teslaris-event-5", BIBO + "presents", uri));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.DOCUMENTS, 31),
            new EntityReference(PushEntityType.EVENTS, 5))));
    }

    @Test
    public void shouldMapPublisherEntityAsReplaceableSharedNode() {
        // Given
        var monograph = document(ExportPublicationType.MONOGRAPH, 40);
        monograph.setMonographType(MonographType.RESEARCH_MONOGRAPH);
        monograph.setPrintIsbn("978-86-7892-000-0");
        monograph.getPublishers().add(new ExportPublisher(english("UNS Press"), 9));

        // When
        var mapping = mapper.map(monograph, target());

        // Then
        var model = mapping.model();
        var uri = NS + "teslaris-document-40";
        var publisher = NS + "teslaris-publisher-9";
        assertTrue(has(model, uri, RDF.type.getURI(), BIBO + "Book"));
        assertTrue(model.contains(model.createResource(uri), model.createProperty(BIBO + "isbn13"),
            "978-86-7892-000-0"));
        assertTrue(has(model, uri, VIVO + "publisher", publisher));
        assertTrue(has(model, publisher, VIVO + "publisherOf", uri));
        assertFalse(model.contains(model.createResource(publisher), RDFS.label));

        assertEquals(1, mapping.sharedNodes().size());
        var node = mapping.sharedNodes().getFirst();
        assertEquals(publisher, node.uri());
        assertTrue(node.replaceWhenChanged());
        assertTrue(has(node.model(), publisher, RDF.type.getURI(), VIVO + "Publisher"));
        assertTrue(node.model().contains(node.model().createResource(publisher), RDFS.label,
            node.model().createLiteral("UNS Press", "en")));
    }

    @Test
    public void shouldMapDisplayPublisherAsNameBasedSharedNode() {
        // Given
        var thesis = document(ExportPublicationType.THESIS, 50);
        thesis.setDisplayPublisher(english("Some Press"));
        var grantor = new ExportOrganisationUnit();
        grantor.setDatabaseId(3);
        thesis.setThesisGrantor(grantor);

        // When
        var mapping = mapper.map(thesis, target());

        // Then
        var model = mapping.model();
        var uri = NS + "teslaris-document-50";
        assertTrue(has(model, uri, RDF.type.getURI(), BIBO + "Thesis"));
        assertTrue(has(model, uri, BIBO + "issuer", NS + "teslaris-ou-3"));

        var node = mapping.sharedNodes().getFirst();
        assertTrue(node.uri().startsWith(NS + "teslaris-external-publisher-"));
        assertFalse(node.replaceWhenChanged());
        assertTrue(has(model, uri, VIVO + "publisher", node.uri()));
    }

    @ParameterizedTest
    @CsvSource({
        "JOURNAL_PUBLICATION,EDITORIAL,http://vivoweb.org/ontology/core#EditorialArticle",
        "JOURNAL_PUBLICATION,CORRECTION,http://purl.org/spar/fabio/Erratum",
        "JOURNAL_PUBLICATION,PREFACE,http://purl.org/ontology/bibo/Article",
        "JOURNAL_PUBLICATION,,http://purl.org/ontology/bibo/Article",
        "PROCEEDINGS_PUBLICATION,INVITED_ABSTRACT_ARTICLE,http://vivoweb.org/ontology/core#Abstract",
        "MONOGRAPH,EDITED_BOOK,http://purl.org/ontology/bibo/EditedBook",
        "MONOGRAPH,WORKING_PAPER,http://vivoweb.org/ontology/core#WorkingPaper",
        "MONOGRAPH,,http://purl.org/ontology/bibo/Book",
        "MONOGRAPH_PUBLICATION,CHAPTER,http://purl.org/ontology/bibo/Chapter",
        "MONOGRAPH_PUBLICATION,,http://purl.org/ontology/bibo/BookSection",
        "PROCEEDINGS,,http://purl.org/ontology/bibo/Proceedings",
        "BOOK_SERIES,,http://purl.org/ontology/bibo/Series",
        "INTELLECTUAL_PROPERTY,PATENT,http://purl.org/ontology/bibo/Patent",
        "INTELLECTUAL_PROPERTY,LICENSE,http://purl.obolibrary.org/obo/IAO_0000030",
        "INTANGIBLE_PRODUCT,DATASET,http://vivoweb.org/ontology/core#Dataset",
        "INTANGIBLE_PRODUCT,METHOD,http://purl.obolibrary.org/obo/IAO_0000030",
        "INTANGIBLE_PRODUCT,,http://purl.obolibrary.org/obo/IAO_0000030",
        "PERFORMANCE_RELATED_OUTPUT,POSTER,http://vivoweb.org/ontology/core#ConferencePoster",
        "PERFORMANCE_RELATED_OUTPUT,LITIGATION,http://purl.obolibrary.org/obo/IAO_0000030",
        "MATERIAL_PRODUCT,PROTOTYPE,http://purl.obolibrary.org/obo/IAO_0000030",
        "GENETIC_MATERIAL,STRAIN,http://purl.obolibrary.org/obo/IAO_0000030"
    })
    public void shouldMapTypeToVivoClass(ExportPublicationType type, String concreteType,
                                         String expectedClass) {
        // Given
        var document = document(type, 60);
        if (concreteType != null) {
            switch (type) {
                case JOURNAL_PUBLICATION -> document.setJournalPublicationType(
                    JournalPublicationType.valueOf(concreteType));
                case PROCEEDINGS_PUBLICATION -> document.setProceedingsPublicationType(
                    ProceedingsPublicationType.valueOf(concreteType));
                case MONOGRAPH -> document.setMonographType(MonographType.valueOf(concreteType));
                case MONOGRAPH_PUBLICATION -> document.setMonographPublicationType(
                    MonographPublicationType.valueOf(concreteType));
                case INTELLECTUAL_PROPERTY -> document.setIntellectualPropertyType(
                    IntellectualPropertyType.valueOf(concreteType));
                case INTANGIBLE_PRODUCT -> document.setIntangibleProductType(
                    IntangibleProductType.valueOf(concreteType));
                case PERFORMANCE_RELATED_OUTPUT -> document.setPerformanceRelatedOutputType(
                    PerformanceRelatedOutputType.valueOf(concreteType));
                case MATERIAL_PRODUCT -> document.setMaterialProductType(
                    MaterialProductType.valueOf(concreteType));
                case GENETIC_MATERIAL -> document.setGeneticMaterialType(
                    GeneticMaterialType.valueOf(concreteType));
                default -> {
                }
            }
        }

        // When
        var model = mapper.toRdf(document, target());

        // Then
        var uri = VivoDocumentMapper.uri(target(), document);
        assertEquals(1,
            model.listObjectsOfProperty(model.createResource(uri), RDF.type).toList().size());
        assertTrue(has(model, uri, RDF.type.getURI(), expectedClass));
    }

    @Test
    public void shouldMapTranslatedMonographWithTwoClassesAndNoBlankNodes() {
        // Given
        var monograph = document(ExportPublicationType.MONOGRAPH, 41);
        monograph.setMonographType(MonographType.TRANSLATION);

        // When
        var model = mapper.toRdf(monograph, target());

        // Then
        var uri = NS + "teslaris-document-41";
        assertTrue(has(model, uri, RDF.type.getURI(), BIBO + "Book"));
        assertTrue(has(model, uri, RDF.type.getURI(), VIVO + "Translation"));
        NTriplesSerializer.toLines(model);
    }

    @Test
    public void shouldSendPatentNumberButNotInternalProductNumber() {
        // Given
        var patent = document(ExportPublicationType.INTELLECTUAL_PROPERTY, 70);
        patent.setIntellectualPropertyType(IntellectualPropertyType.PATENT);
        patent.setNumber("RS-12345");
        var software = document(ExportPublicationType.INTANGIBLE_PRODUCT, 71);
        software.setIntangibleProductType(IntangibleProductType.SOFTWARE);
        software.setNumber("INT-7");

        // When
        var patentModel = mapper.toRdf(patent, target());
        var softwareModel = mapper.toRdf(software, target());

        // Then
        var patentUri = patentModel.createResource(NS + "teslaris-document-70");
        assertTrue(patentModel.contains(patentUri,
            patentModel.createProperty(VIVO + "patentNumber"), "RS-12345"));
        assertFalse(patentModel.contains(patentUri,
            patentModel.createProperty(BIBO + "number")));
        assertTrue(patentModel.contains(patentModel.createResource(
                NS + "teslaris-document-70-url-teslaris"),
            patentModel.createProperty(VCARD + "url"),
            patentModel.createTypedLiteral(
                "http://localhost:5173/sr/scientific-results/intellectual-property/70",
                XSDDatatype.XSDanyURI)));

        assertFalse(softwareModel.contains(
            softwareModel.createResource(NS + "teslaris-document-71"),
            softwareModel.createProperty(BIBO + "number")));
    }

    // These VIVO classes are not information content entities, so VIVO would not show the
    // authors without the second class.
    @ParameterizedTest
    @CsvSource({
        "INTANGIBLE_PRODUCT,SOFTWARE,http://purl.obolibrary.org/obo/ERO_0000071",
        "PERFORMANCE_RELATED_OUTPUT,THEATRIC_PLAY,http://purl.org/ontology/bibo/Performance",
        "PERFORMANCE_RELATED_OUTPUT,MUSICAL_PERFORMANCE,http://purl.org/ontology/bibo/Performance",
        "PERFORMANCE_RELATED_OUTPUT,ARTISTIC_EXHIBITION,http://vivoweb.org/ontology/core#Exhibit",
        "PERFORMANCE_RELATED_OUTPUT,NON_RESEARCH_PRESENTATION,http://vivoweb.org/ontology/core#Presentation",
        "PERFORMANCE_RELATED_OUTPUT,BROADCAST_INTERVIEW,http://purl.org/ontology/bibo/Interview",
        "PERFORMANCE_RELATED_OUTPUT,TEXT_INTERVIEW,http://purl.org/ontology/bibo/Interview"
    })
    public void shouldAddInformationContentEntityForOutputsOutsideIt(ExportPublicationType type,
                                                                     String concreteType,
                                                                     String expectedClass) {
        // Given
        var document = document(type, 80);
        if (type == ExportPublicationType.INTANGIBLE_PRODUCT) {
            document.setIntangibleProductType(IntangibleProductType.valueOf(concreteType));
        } else {
            document.setPerformanceRelatedOutputType(
                PerformanceRelatedOutputType.valueOf(concreteType));
        }

        // When
        var model = mapper.toRdf(document, target());

        // Then
        var uri = NS + "teslaris-document-80";
        assertEquals(2,
            model.listObjectsOfProperty(model.createResource(uri), RDF.type).toList().size());
        assertTrue(has(model, uri, RDF.type.getURI(), expectedClass));
        assertTrue(has(model, uri, RDF.type.getURI(),
            "http://purl.obolibrary.org/obo/IAO_0000030"));
    }

    @Test
    public void shouldMapPresentationOfInvitedSpeakerAsInvitedTalk() {
        // Given
        var presentation = document(ExportPublicationType.PERFORMANCE_RELATED_OUTPUT, 81);
        presentation.setPerformanceRelatedOutputType(
            PerformanceRelatedOutputType.NON_RESEARCH_PRESENTATION);
        presentation.setInvitedTalk(true);

        // When
        var model = mapper.toRdf(presentation, target());

        // Then
        var uri = NS + "teslaris-document-81";
        assertEquals(2,
            model.listObjectsOfProperty(model.createResource(uri), RDF.type).toList().size());
        assertTrue(has(model, uri, RDF.type.getURI(), VIVO + "InvitedTalk"));
        assertTrue(has(model, uri, RDF.type.getURI(),
            "http://purl.obolibrary.org/obo/IAO_0000030"));
    }

    @Test
    public void shouldLinkSubjectAreas() {
        // Given
        var document = article();
        document.setResearchAreaIds(new ArrayList<>(List.of(9, 4)));
        // When
        var mapping = mapper.map(document, target());

        // Then
        var model = mapping.model();
        var subject = model.createResource(VivoDocumentMapper.uri(target(), document));
        var area = model.createResource(NS + "teslaris-research-area-4");
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"), area));
        assertTrue(model.contains(area,
            model.createProperty("http://vivoweb.org/ontology/core#subjectAreaOf"), subject));
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"),
            model.createResource(NS + "teslaris-research-area-9")));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.RESEARCH_AREAS, 4),
            new EntityReference(PushEntityType.RESEARCH_AREAS, 9))));
    }
}
