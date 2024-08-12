package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import rs.teslaris.core.exporter.model.common.ExportContribution;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
import rs.teslaris.core.exporter.model.common.ExportPublisher;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.publication.PartOf;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;
import rs.teslaris.core.importer.model.oaipmh.publication.PublishedIn;
import rs.teslaris.core.importer.model.oaipmh.publication.Publisher;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;

public class ExportDocumentConverter extends ExportConverterBase {

    public static ExportDocument toCommonExportModel(Dataset dataset) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.DATASET);

        setBaseFields(commonExportDocument, dataset);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, dataset);

        commonExportDocument.setNumber(dataset.getInternalNumber());
        if (Objects.nonNull(dataset.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    dataset.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Software software) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.SOFTWARE);

        setBaseFields(commonExportDocument, software);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, software);

        commonExportDocument.setNumber(software.getInternalNumber());
        if (Objects.nonNull(software.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    software.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Patent patent) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PATENT);

        setBaseFields(commonExportDocument, patent);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, patent);

        commonExportDocument.setNumber(patent.getNumber());
        if (Objects.nonNull(patent.getPublisher())) {
            commonExportDocument.getPublishers().add(new ExportPublisher(
                ExportMultilingualContentConverter.toCommonExportModel(
                    patent.getPublisher().getName())));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(JournalPublication journalPublication) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.JOURNAL_PUBLICATION);

        setBaseFields(commonExportDocument, journalPublication);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, journalPublication);

        commonExportDocument.setJournalPublicationType(
            journalPublication.getJournalPublicationType());
        commonExportDocument.setStartPage(journalPublication.getStartPage());
        commonExportDocument.setEndPage(journalPublication.getEndPage());
        commonExportDocument.setNumber(journalPublication.getArticleNumber());
        commonExportDocument.setVolume(journalPublication.getVolume());
        commonExportDocument.setIssue(journalPublication.getIssue());
        if (Objects.nonNull(journalPublication.getJournal())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                journalPublication.getJournal()));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Proceedings proceedings) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PROCEEDINGS);

        setBaseFields(commonExportDocument, proceedings);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, proceedings);

        commonExportDocument.setEIsbn(proceedings.getEISBN());
        commonExportDocument.setPrintIsbn(proceedings.getPrintISBN());
        commonExportDocument.setVolume(proceedings.getPublicationSeriesVolume());
        commonExportDocument.setIssue(proceedings.getPublicationSeriesIssue());
        proceedings.getLanguages().forEach(languageTag -> {
            commonExportDocument.getLanguageTags().add(languageTag.getLanguageTag());
        });

        if (Objects.nonNull(proceedings.getPublicationSeries())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                proceedings.getPublicationSeries()));
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
        ProceedingsPublication proceedingsPublication) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.PROCEEDINGS_PUBLICATION);

        setBaseFields(commonExportDocument, proceedingsPublication);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, proceedingsPublication);

        commonExportDocument.setProceedingsPublicationType(
            proceedingsPublication.getProceedingsPublicationType());
        commonExportDocument.setStartPage(proceedingsPublication.getStartPage());
        commonExportDocument.setEndPage(proceedingsPublication.getEndPage());
        commonExportDocument.setNumber(proceedingsPublication.getArticleNumber());
        if (Objects.nonNull(proceedingsPublication.getProceedings())) {
            commonExportDocument.setProceedings(ExportDocumentConverter.toCommonExportModel(
                proceedingsPublication.getProceedings()));
        }

        return commonExportDocument;
    }

    public static ExportDocument toCommonExportModel(Monograph monograph) {
        var commonExportDocument = new ExportDocument();
        commonExportDocument.setType(ExportPublicationType.MONOGRAPH);

        setBaseFields(commonExportDocument, monograph);
        if (commonExportDocument.getDeleted()) {
            return commonExportDocument;
        }

        setCommonFields(commonExportDocument, monograph);

        commonExportDocument.setMonographType(monograph.getMonographType());
        commonExportDocument.setEIsbn(monograph.getEISBN());
        commonExportDocument.setPrintIsbn(monograph.getPrintISBN());
        commonExportDocument.setVolume(monograph.getVolume());
        commonExportDocument.setNumber(monograph.getNumber());

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            commonExportDocument.setJournal(ExportPublicationSeriesConverter.toCommonExportModel(
                monograph.getPublicationSeries()));
            commonExportDocument.setEdition(monograph.getPublicationSeries().getTitle().stream()
                .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get().getContent());
        }

        monograph.getLanguages().forEach(languageTag -> {
            commonExportDocument.getLanguageTags().add(languageTag.getLanguageTag());
        });

        // TODO: Do we need research areas?

        return commonExportDocument;
    }

    private static void setCommonFields(ExportDocument commonExportDocument, Document document) {
        commonExportDocument.setTitle(
            ExportMultilingualContentConverter.toCommonExportModel(document.getTitle()));
        commonExportDocument.setSubtitle(
            ExportMultilingualContentConverter.toCommonExportModel(document.getSubTitle()));
        commonExportDocument.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(document.getDescription()));
        commonExportDocument.setKeywords(
            ExportMultilingualContentConverter.toCommonExportModel(document.getKeywords()));

        document.getContributors().forEach(contributor -> {
            switch (contributor.getContributionType()) {
                case AUTHOR:
                    commonExportDocument.getAuthors().add(createExportContribution(contributor));
                    break;
                case EDITOR:
                    commonExportDocument.getEditors().add(createExportContribution(contributor));
                    break;
            }
        });

        commonExportDocument.setUris(document.getUris().stream().toList());
        commonExportDocument.setDocumentDate(document.getDocumentDate());
        commonExportDocument.setDoi(document.getDoi());
        commonExportDocument.setScopus(document.getScopusId());
        commonExportDocument.setOldId(document.getOldId());

        if (Objects.nonNull(document.getEvent())) {
            commonExportDocument.setEvent(
                ExportEventConverter.toCommonExportModel(document.getEvent()));
        }

        commonExportDocument.getRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(document, false));
        commonExportDocument.getActivelyRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(document, true));
    }

    private static ExportContribution createExportContribution(
        PersonDocumentContribution contribution) {
        var exportContribution = new ExportContribution();
        exportContribution.setDisplayName(
            contribution.getAffiliationStatement().getDisplayPersonName().toString());
        if (Objects.nonNull(contribution.getPerson())) {
            exportContribution.setPerson(
                ExportPersonConverter.toCommonExportModel(contribution.getPerson()));
        }

        return exportContribution;
    }

    private static Set<Integer> getRelatedInstitutions(Document document, boolean onlyActive) {
        var relations = new HashSet<Integer>();
        document.getContributors().forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson())) {
                relations.addAll(ExportPersonConverter.getRelatedInstitutions(
                    contribution.getPerson(), onlyActive));
            }
        });
        return relations;
    }

    public static Publication toOpenaireModel(ExportDocument exportDocument) {
        var openairePublication = new Publication();
        openairePublication.setOldId("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        openairePublication.setTitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));
        openairePublication.setSubtitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getSubtitle()));

        if (!exportDocument.getLanguageTags().isEmpty()) {
            openairePublication.setLanguage(
                exportDocument.getLanguageTags().getFirst()); // is this ok?
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
        openairePublication.setIssn(exportDocument.getEIssn()); // is this ok?
        openairePublication.setIsbn(exportDocument.getEIsbn()); // is this ok?
        openairePublication.setAccess("OPEN"); // is this ok?
        openairePublication.setEdition(exportDocument.getEdition());

        if (Objects.nonNull(exportDocument.getJournal())) {
            openairePublication.setPublishedIn(new PublishedIn(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getJournal())));
        }

        if (Objects.nonNull(
            exportDocument.getProceedings())) { // TODO: same for monographs when we add monograph publications
            var partOf = new PartOf();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                exportDocument.getProceedings().getTitle().stream(),
                Function.identity(),
                partOf::setDisplayName
            );
            partOf.setPublication(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getProceedings()));
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
            // TODO: what to do with publisher's OU?
            openairePublication.getPublishers().add(openairePublisher);
        });

        return openairePublication;
    }
}
