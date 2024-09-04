package rs.teslaris.core.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import rs.teslaris.core.exporter.model.common.ExportContribution;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.model.common.ExportPersonName;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
import rs.teslaris.core.exporter.model.common.ExportPublisher;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.dspaceinternal.Dim;
import rs.teslaris.core.importer.model.oaipmh.dspaceinternal.DimField;
import rs.teslaris.core.importer.model.oaipmh.dublincore.Contributor;
import rs.teslaris.core.importer.model.oaipmh.dublincore.DC;
import rs.teslaris.core.importer.model.oaipmh.etdms.Degree;
import rs.teslaris.core.importer.model.oaipmh.etdms.ETDMSThesis;
import rs.teslaris.core.importer.model.oaipmh.etdms.LevelType;
import rs.teslaris.core.importer.model.oaipmh.etdms.ThesisType;
import rs.teslaris.core.importer.model.oaipmh.publication.PartOf;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;
import rs.teslaris.core.importer.model.oaipmh.publication.PublishedIn;
import rs.teslaris.core.importer.model.oaipmh.publication.Publisher;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;

public class ExportDocumentConverter extends ExportConverterBase {

    public static ExportDocument toCommonExportModel(Dataset dataset, boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.DATASET);

        setBaseFields(commonExportDocument, dataset);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, dataset, computeRelations);

        commonExportDocument.setNumber(dataset.getInternalNumber());
        if (Objects.nonNull(dataset.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    dataset.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Software software, boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.SOFTWARE);

        setBaseFields(commonExportDocument, software);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, software, computeRelations);

        commonExportDocument.setNumber(software.getInternalNumber());
        if (Objects.nonNull(software.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    software.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Patent patent, boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PATENT);

        setBaseFields(commonExportDocument, patent);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, patent, computeRelations);

        commonExportDocument.setNumber(patent.getNumber());
        if (Objects.nonNull(patent.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    patent.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(JournalPublication journalPublication,
                                                     boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.JOURNAL_PUBLICATION);

        setBaseFields(commonExportDocument, journalPublication);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, journalPublication, computeRelations);

        commonExportDocument.setJournalPublicationType(
            journalPublication.getJournalPublicationType());
        commonExportDocument.setStartPage(journalPublication.getStartPage());
        commonExportDocument.setEndPage(journalPublication.getEndPage());
        commonExportDocument.setNumber(journalPublication.getArticleNumber());
        commonExportDocument.setVolume(journalPublication.getVolume());
        commonExportDocument.setIssue(journalPublication.getIssue());
        if (Objects.nonNull(journalPublication.getJournal())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                journalPublication.getJournal(), false));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Proceedings proceedings,
                                                     boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PROCEEDINGS);

        setBaseFields(commonExportDocument, proceedings);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, proceedings, computeRelations);

        commonExportDocument.setEIsbn(proceedings.getEISBN());
        commonExportDocument.setPrintIsbn(proceedings.getPrintISBN());
        commonExportDocument.setVolume(proceedings.getPublicationSeriesVolume());
        commonExportDocument.setIssue(proceedings.getPublicationSeriesIssue());
        proceedings.getLanguages().forEach(languageTag -> {
            commonExportDocument.getLanguageTags().add(languageTag.getLanguageTag());
        });

        if (Objects.nonNull(proceedings.getPublicationSeries())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                proceedings.getPublicationSeries(), false));
            commonExportDocument.setEdition(proceedings.getPublicationSeries().getTitle().stream()
                .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get().getContent());
        }
        if (Objects.nonNull(proceedings.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    proceedings.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(
        ProceedingsPublication proceedingsPublication, boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PROCEEDINGS_PUBLICATION);

        setBaseFields(commonExportDocument, proceedingsPublication);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, proceedingsPublication, computeRelations);

        commonExportDocument.setProceedingsPublicationType(
            proceedingsPublication.getProceedingsPublicationType());
        commonExportDocument.setStartPage(proceedingsPublication.getStartPage());
        commonExportDocument.setEndPage(proceedingsPublication.getEndPage());
        commonExportDocument.setNumber(proceedingsPublication.getArticleNumber());
        if (Objects.nonNull(proceedingsPublication.getProceedings())) {
            commonExportDocument.setProceedings(ExportDocumentConverter.toCommonExportModel(
                proceedingsPublication.getProceedings(), false));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Monograph monograph,
                                                     boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.MONOGRAPH);

        setBaseFields(commonExportDocument, monograph);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, monograph, computeRelations);

        commonExportDocument.setMonographType(monograph.getMonographType());
        commonExportDocument.setEIsbn(monograph.getEISBN());
        commonExportDocument.setPrintIsbn(monograph.getPrintISBN());
        commonExportDocument.setVolume(monograph.getVolume());
        commonExportDocument.setNumber(monograph.getNumber());

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                monograph.getPublicationSeries(), false));
            commonExportDocument.setEdition(monograph.getPublicationSeries().getTitle().stream()
                .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get().getContent());
        }

        monograph.getLanguages().forEach(languageTag -> {
            commonExportDocument.getLanguageTags().add(languageTag.getLanguageTag());
        });

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(MonographPublication monographPublication,
                                                     boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.MONOGRAPH_PUBLICATION);

        setBaseFields(commonExportDocument, monographPublication);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, monographPublication, computeRelations);

        commonExportDocument.setMonographPublicationType(
            monographPublication.getMonographPublicationType());
        commonExportDocument.setStartPage(monographPublication.getStartPage());
        commonExportDocument.setEndPage(monographPublication.getEndPage());
        commonExportDocument.setNumber(monographPublication.getArticleNumber());

        if (Objects.nonNull(monographPublication.getMonograph())) {
            commonExportDocument.setMonograph(
                ExportDocumentConverter.toCommonExportModel(monographPublication.getMonograph(),
                    false));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Thesis thesis, boolean computeRelations) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.THESIS);

        setBaseFields(commonExportDocument, thesis);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, thesis, computeRelations);

        commonExportDocument.setThesisType(thesis.getThesisType());
        commonExportDocument.setThesisGrantor(
            ExportOrganisationUnitConverter.toCommonExportModel(thesis.getOrganisationUnit(),
                false));

        thesis.getLanguages().forEach(languageTag -> {
            commonExportDocument.getLanguageTags().add(languageTag.getLanguageTag());
        });

        if (Objects.nonNull(thesis.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    thesis.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    private static void setCommonFields(ExportDocument commonExportDocument, Document document,
                                        boolean computeRelations) {
        commonExportDocument.setTitle(
            ExportMultilingualContentConverter.toCommonExportModel(document.getTitle()));
        commonExportDocument.setSubtitle(
            ExportMultilingualContentConverter.toCommonExportModel(document.getSubTitle()));
        commonExportDocument.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(document.getDescription()));
        commonExportDocument.setKeywords(
            ExportMultilingualContentConverter.toCommonExportModel(document.getKeywords()));

        document.getContributors().stream()
            .sorted(Comparator.comparingInt(PersonContribution::getOrderNumber))
            .forEach(contributor -> {
                switch (contributor.getContributionType()) {
                    case AUTHOR:
                        commonExportDocument.getAuthors()
                            .add(createExportContribution(contributor));
                        break;
                    case EDITOR:
                        commonExportDocument.getEditors()
                            .add(createExportContribution(contributor));
                        break;
                    case ADVISOR:
                        commonExportDocument.getAdvisors()
                            .add(createExportContribution(contributor));
                        break;
                    case BOARD_MEMBER:
                        commonExportDocument.getBoardMembers()
                            .add(createExportContribution(contributor));
                        break;
                }
            });

        commonExportDocument.setUris(document.getUris().stream().toList());
        commonExportDocument.setDocumentDate(document.getDocumentDate());
        commonExportDocument.setDoi(document.getDoi());
        commonExportDocument.setScopus(document.getScopusId());
        commonExportDocument.setOldId(document.getOldId());

        if (Objects.nonNull(document.getEvent())) {
//            commonExportDocument.setEvent(
//                ExportEventConverter.toCommonExportModel(document.getEvent(), false));
            var event = new ExportEvent();
            event.setDatabaseId(document.getEvent().getId());
            event.setName(ExportMultilingualContentConverter.toCommonExportModel(
                document.getEvent().getName()));
        }

        if (computeRelations) {
            var relations = getRelatedInstitutions(document);
            commonExportDocument.getRelatedInstitutionIds().addAll(relations);
            commonExportDocument.getActivelyRelatedInstitutionIds().addAll(relations);
        }

        commonExportDocument.setOpenAccess(false);
        document.getFileItems().forEach(file -> {
            commonExportDocument.getFileFormats().add(file.getMimeType());
            if (file.getLicense().equals(License.OPEN_ACCESS)) {
                commonExportDocument.setOpenAccess(true);
            }
        });
    }

    private static ExportContribution createExportContribution(
        PersonDocumentContribution contribution) {
        var exportContribution = new ExportContribution();
        exportContribution.setDisplayName(
            contribution.getAffiliationStatement().getDisplayPersonName().toString());
        if (Objects.nonNull(contribution.getPerson())) {
//            exportContribution.setPerson(
//                ExportPersonConverter.toCommonExportModel(contribution.getPerson(), false));
            var person = new ExportPerson();
            person.setDatabaseId(contribution.getPerson().getId());
            person.setName(new ExportPersonName(contribution.getPerson().getName().getFirstname(),
                contribution.getPerson().getName().getOtherName(),
                contribution.getPerson().getName().getLastname()));
            exportContribution.setPerson(person);
        }

        return exportContribution;
    }

    private static Set<Integer> getRelatedInstitutions(Document document) {
        var relations = new HashSet<Integer>();
        document.getContributors().forEach(contribution -> {
            contribution.getInstitutions().forEach(institution -> {
                relations.add(institution.getId());
            });
        });
        return relations;
    }

    public static Publication toOpenaireModel(ExportDocument exportDocument) {
        var openairePublication = new Publication();
        openairePublication.setOldId("Publications/(TESLARIS)" + exportDocument.getDatabaseId());
        openairePublication.setTitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));
        openairePublication.setSubtitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getSubtitle()));

        openairePublication.setType(inferPublicationCOARType(exportDocument.getType()));

        if (!exportDocument.getLanguageTags().isEmpty()) {
            openairePublication.setLanguage(
                exportDocument.getLanguageTags().getFirst());
        }

        setDocumentDate(exportDocument.getDocumentDate(), openairePublication::setPublicationDate);

        openairePublication.setNumber(exportDocument.getNumber());
        openairePublication.setVolume(exportDocument.getVolume());
        openairePublication.setIssue(exportDocument.getIssue());
        openairePublication.setStartPage(exportDocument.getStartPage());
        openairePublication.setEndPage(exportDocument.getEndPage());
        openairePublication.setUrl(exportDocument.getUris());
        openairePublication.setDoi(exportDocument.getDoi());
        openairePublication.setScpNumber(exportDocument.getScopus());
        openairePublication.setIssn(
            Objects.nonNull(exportDocument.getEIssn()) && !exportDocument.getEIssn().isBlank() ?
                exportDocument.getEIssn() : exportDocument.getPrintIssn());
        openairePublication.setIsbn(
            Objects.nonNull(exportDocument.getEIsbn()) && !exportDocument.getEIsbn().isBlank() ?
                exportDocument.getEIsbn() : exportDocument.getPrintIsbn());
        openairePublication.setEdition(exportDocument.getEdition());

        openairePublication.setAccess(
            exportDocument.getOpenAccess() ? "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        if (Objects.nonNull(exportDocument.getJournal())) {
            openairePublication.setPublishedIn(new PublishedIn(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getJournal())));
        }

        if (Objects.nonNull(exportDocument.getProceedings())) {
            var partOf = new PartOf();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                exportDocument.getProceedings().getTitle().stream(),
                Function.identity(),
                partOf::setDisplayName
            );
            partOf.setPublication(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getProceedings()));
            openairePublication.setPartOf(partOf);
        } else if (Objects.nonNull(exportDocument.getMonograph())) {
            var partOf = new PartOf();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                exportDocument.getMonograph().getTitle().stream(),
                Function.identity(),
                partOf::setDisplayName
            );
            partOf.setPublication(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getMonograph()));
            openairePublication.setPartOf(partOf);
        }

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getDescription().stream(),
            Function.identity(),
            openairePublication::set_abstract
        );

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getKeywords().stream(),
            content -> List.of(content.split("\n")),
            openairePublication::setKeywords
        );

        openairePublication.setAuthors(new ArrayList<>());
        exportDocument.getAuthors().forEach(contribution -> {
            openairePublication.getAuthors().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        openairePublication.setEditors(new ArrayList<>());
        exportDocument.getEditors().forEach(contribution -> {
            openairePublication.getEditors().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        openairePublication.setPublishers(new ArrayList<>());
        exportDocument.getPublishers().forEach(publisher -> {
            var openairePublisher = new Publisher();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                publisher.getName().stream(),
                Function.identity(),
                openairePublisher::setDisplayName
            );
            openairePublication.getPublishers().add(openairePublisher);
        });

        return openairePublication;
    }

    public static DC toDCModel(ExportDocument exportDocument) {
        var dcPublication = new DC();

        setDCCommonFields(exportDocument, dcPublication);

        return dcPublication;
    }

    public static ETDMSThesis toETDMSModel(ExportDocument exportDocument) {
        var thesisType = new ThesisType();

        setDCCommonFields(exportDocument, thesisType);

        if (exportDocument.getType().equals(ExportPublicationType.THESIS)) {
            var degree = new Degree();
            addContentToList(
                exportDocument.getThesisGrantor().getName(),
                ExportMultilingualContent::getContent,
                content -> degree.getGrantor().add(content)
            );
            degree.setLevel(
                new LevelType(String.valueOf(exportDocument.getThesisType().ordinal() % 3)));
            degree.getName().add(exportDocument.getThesisType().name());
            thesisType.setDegree(degree);
        }

        var thesis = new ETDMSThesis();
        thesis.setThesisType(thesisType);
        return thesis;
    }

    public static Dim toDIMModel(ExportDocument exportDocument) {
        var dimPublication = new Dim();

        exportDocument.getTitle().forEach(mc -> {
            var field = new DimField();
            field.setMdschema("dc");
            field.setElement("title");
            field.setLanguage(mc.getLanguageTag());
            field.setValue(mc.getContent());
            dimPublication.getFields().add(field);
        });

        exportDocument.getAuthors().forEach(author -> {
            var field = new DimField();
            if (Objects.nonNull(author.getPerson().getOrcid()) &&
                !author.getPerson().getOrcid().isBlank()) {
                field.setAuthority(author.getPerson().getOrcid());
            }
            field.setMdschema("dc");
            field.setElement("creator");
            field.setValue(author.getDisplayName());
            dimPublication.getFields().add(field);
        });

        exportDocument.getEditors().forEach(editor -> {
            var field = new DimField();
            if (Objects.nonNull(editor.getPerson().getOrcid()) &&
                !editor.getPerson().getOrcid().isBlank()) {
                field.setAuthority(editor.getPerson().getOrcid());
            }
            field.setMdschema("dc");
            field.setElement("contributor");
            field.setQualifier("editor");
            field.setValue(editor.getDisplayName());
            dimPublication.getFields().add(field);
        });

        exportDocument.getAdvisors().forEach(advisor -> {
            var field = new DimField();
            if (Objects.nonNull(advisor.getPerson().getOrcid()) &&
                !advisor.getPerson().getOrcid().isBlank()) {
                field.setAuthority(advisor.getPerson().getOrcid());
            }
            field.setMdschema("dc");
            field.setElement("contributor");
            field.setQualifier("advisor");
            field.setValue(advisor.getDisplayName());
            dimPublication.getFields().add(field);
        });

        exportDocument.getBoardMembers().forEach(boardMember -> {
            var field = new DimField();
            if (Objects.nonNull(boardMember.getPerson().getOrcid()) &&
                !boardMember.getPerson().getOrcid().isBlank()) {
                field.setAuthority(boardMember.getPerson().getOrcid());
            }
            field.setMdschema("dc");
            field.setElement("contributor");
            field.setQualifier("board member");
            field.setValue(boardMember.getDisplayName());
            dimPublication.getFields().add(field);
        });

        exportDocument.getKeywords().forEach(mc -> {
            var field = new DimField();
            field.setMdschema("dc");
            field.setElement("subject");
            field.setLanguage(mc.getLanguageTag());
            field.setValue(mc.getContent().replace("\n", ", "));
            dimPublication.getFields().add(field);
        });

        exportDocument.getDescription().forEach(mc -> {
            var field = new DimField();
            field.setMdschema("dc");
            field.setElement("description");
            field.setLanguage(mc.getLanguageTag());
            field.setValue(mc.getContent().replace("\n", ", "));
            dimPublication.getFields().add(field);
        });

        exportDocument.getPublishers().forEach(publisher -> {
            publisher.getName().forEach(mc -> {
                var field = new DimField();
                field.setMdschema("dc");
                field.setElement("publisher");
                field.setLanguage(mc.getLanguageTag());
                field.setValue(mc.getContent());
                dimPublication.getFields().add(field);
            });
        });

        exportDocument.getLanguageTags().forEach(languageTag -> {
            var field = new DimField();
            field.setMdschema("dc");
            field.setElement("language");
            field.setQualifier("iso");
            field.setValue(languageTag);
            dimPublication.getFields().add(field);
        });

        dimPublication.getFields().add(new DimField("dc", "date", "issued", null, null, null,
            exportDocument.getDocumentDate()));
        dimPublication.getFields().add(
            new DimField("dc", "type", null, "en", null, null, exportDocument.getType().name()));

        if (Objects.nonNull(exportDocument.getThesisGrantor())) {
            exportDocument.getThesisGrantor().getName().forEach(mc -> {
                var field = new DimField();
                field.setMdschema("dc");
                field.setElement("source");
                field.setLanguage(mc.getLanguageTag());
                field.setValue(mc.getContent());
                dimPublication.getFields().add(field);
            });
        }
        dimPublication.getFields().add(new DimField("dc", "source", null, null, null, null,
            repositoryName + " (" + baseFrontendUrl + ")"));

        dimPublication.getFields().add(new DimField("dc", "rights", null, null, null, null,
            exportDocument.getOpenAccess() ? "Attribution-NonCommercial" :
                "Attribution-NonCommercial-NoDerivs")); // TODO: improve this

        clientLanguages.forEach(lang -> {
            dimPublication.getFields()
                .add(new DimField("dc", "identifier", "uri", lang, null, null,
                    baseFrontendUrl + lang + "/scientific-results/" +
                        getConcreteEntityPath(exportDocument.getType()) + "/" +
                        exportDocument.getDatabaseId()));
        });

        if (Objects.nonNull(exportDocument.getEIssn()) && !exportDocument.getEIssn().isBlank()) {
            dimPublication.getFields()
                .add(new DimField("dc", "identifier", "issn", null, null, null,
                    exportDocument.getEIssn()));
        }

        dimPublication.getFields().add(new DimField("dc", "citation", "spage", null, null, null,
            exportDocument.getStartPage()));
        dimPublication.getFields().add(new DimField("dc", "citation", "epage", null, null, null,
            exportDocument.getEndPage()));
        dimPublication.getFields().add(new DimField("dc", "citation", "volume", null, null, null,
            exportDocument.getVolume()));

        // TODO: add rank when (e.g. M21) we add support for it!

        return dimPublication;
    }

    private static void setDCCommonFields(ExportDocument exportDocument, DC dcPublication) {
        dcPublication.getDate().add(exportDocument.getDocumentDate());
        dcPublication.getSource().add(repositoryName);

        dcPublication.getIdentifier().add("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        // TODO: support other identifiers (if applicable)

        dcPublication.getType().add(exportDocument.getType().name());

        clientLanguages.forEach(lang -> {
            dcPublication.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-results/" +
                    getConcreteEntityPath(exportDocument.getType()) + "/" +
                    exportDocument.getDatabaseId());
        });

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            content -> dcPublication.getTitle().add(content)
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcPublication.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getEditors(),
            ExportContribution::getDisplayName,
            content -> dcPublication.getContributor().add(new Contributor(content, "editor"))
        );

        addContentToList(
            exportDocument.getAdvisors(),
            ExportContribution::getDisplayName,
            content -> dcPublication.getContributor().add(new Contributor(content, "advisor"))
        );

        addContentToList(
            exportDocument.getBoardMembers(),
            ExportContribution::getDisplayName,
            content -> dcPublication.getContributor().add(new Contributor(content, "board_member"))
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            content -> dcPublication.getDescription().add(content)
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            content -> dcPublication.getSubject().add(content.replace("\n", "; "))
        );

        addContentToList(
            exportDocument.getLanguageTags(),
            Function.identity(),
            content -> dcPublication.getLanguage().add(content)
        );

        exportDocument.getPublishers().forEach(publisher -> {
            publisher.getName().forEach(name -> {
                dcPublication.getPublisher().add(name.getContent());
            });
        });

        addContentToList(
            exportDocument.getFileFormats(),
            Functions.identity(),
            content -> dcPublication.getFormat().add(content)
        );

        if (Objects.nonNull(exportDocument.getDoi()) && !exportDocument.getDoi().isBlank()) {
            dcPublication.getRelation()
                .add("info:eu-repo/semantics/altIdentifier/doi/" + exportDocument.getDoi());
        }

        dcPublication.getRights().add(
            exportDocument.getOpenAccess() ? "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcPublication.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");
    }
}
