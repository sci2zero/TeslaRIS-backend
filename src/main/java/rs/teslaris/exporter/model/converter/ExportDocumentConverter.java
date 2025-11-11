package rs.teslaris.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.model.oaipmh.dspaceinternal.Dim;
import rs.teslaris.core.model.oaipmh.dspaceinternal.DimField;
import rs.teslaris.core.model.oaipmh.dublincore.Contributor;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.etdms.Degree;
import rs.teslaris.core.model.oaipmh.etdms.ETDMSThesis;
import rs.teslaris.core.model.oaipmh.etdms.LevelType;
import rs.teslaris.core.model.oaipmh.etdms.ThesisType;
import rs.teslaris.core.model.oaipmh.marc21.ControlField;
import rs.teslaris.core.model.oaipmh.marc21.DataField;
import rs.teslaris.core.model.oaipmh.marc21.Marc21;
import rs.teslaris.core.model.oaipmh.marc21.SubField;
import rs.teslaris.core.model.oaipmh.publication.Institution;
import rs.teslaris.core.model.oaipmh.publication.PartOf;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.model.oaipmh.publication.PublishedIn;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportDocumentFile;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.common.ExportPublisher;

@Component
@Slf4j
public class ExportDocumentConverter extends ExportConverterBase {

    private static DocumentRepository documentRepository;

    private static DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private static ThesisResearchOutputRepository thesisResearchOutputRepository;


    @Autowired
    public ExportDocumentConverter(DocumentRepository documentRepository,
                                   DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                   ThesisResearchOutputRepository thesisResearchOutputRepository) {
        ExportDocumentConverter.documentRepository = documentRepository;
        ExportDocumentConverter.documentPublicationIndexRepository =
            documentPublicationIndexRepository;
        ExportDocumentConverter.thesisResearchOutputRepository = thesisResearchOutputRepository;
    }

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

        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            commonExportDocument.setThesisGrantor(
                ExportOrganisationUnitConverter.toCommonExportModel(thesis.getOrganisationUnit(),
                    false));
        } else if (Objects.nonNull(thesis.getExternalOrganisationUnitName())) {
            commonExportDocument.setExternalThesisGrantorName(
                ExportMultilingualContentConverter.toCommonExportModel(
                    thesis.getExternalOrganisationUnitName()));
        } else {
            log.error("Thesis with ID {} does not seem to have any thesis grantor information.",
                commonExportDocument.getDatabaseId());
        }

        if (Objects.nonNull(thesis.getLanguage())) {
            commonExportDocument.getLanguageTags().add(thesis.getLanguage().getLanguageCode());
        }

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

        commonExportDocument.setUris(new ArrayList<>(document.getUris().stream().toList()));
        document.getFileItems().forEach(fileItem -> {
            if (fileItem.getIsVerifiedData() &&
                fileItem.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                commonExportDocument.getUris()
                    .add(baseFrontendUrl + "api/file/" + fileItem.getServerFilename());
            }
        });
        document.getProofs().forEach(fileItem -> {
            if (fileItem.getIsVerifiedData() &&
                fileItem.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                commonExportDocument.getUris()
                    .add(baseFrontendUrl + "api/file/" + fileItem.getServerFilename());
            }
        });

        commonExportDocument.setDocumentDate(document.getDocumentDate());
        commonExportDocument.setDoi(document.getDoi());
        commonExportDocument.setScopus(document.getScopusId());
        commonExportDocument.setOpenAlex(document.getOpenAlexId());
        commonExportDocument.getOldIds().addAll(document.getOldIds());

        if (Objects.nonNull(document.getEvent())) {
            var event = new ExportEvent();
            event.setDatabaseId(document.getEvent().getId());
            event.setOldIds(document.getEvent().getOldIds());
            event.setName(ExportMultilingualContentConverter.toCommonExportModel(
                document.getEvent().getName()));
            commonExportDocument.setEvent(event);
        }

        if (computeRelations) {
            var relations = getRelatedInstitutions(document);
            commonExportDocument.getRelatedInstitutionIds().addAll(relations);
            commonExportDocument.getActivelyRelatedInstitutionIds().addAll(relations);
        }

        commonExportDocument.setOpenAccess(false);
        document.getFileItems().forEach(file -> {
            commonExportDocument.getFileFormats().add(file.getMimeType());
            if (file.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                commonExportDocument.setOpenAccess(true);
            }

            addDocumentFileInformation(commonExportDocument, file);
        });

        if (document instanceof Thesis) {
            ((Thesis) document).getPreliminaryFiles().forEach(
                preliminaryFile -> addDocumentFileInformation(commonExportDocument,
                    preliminaryFile));

            commonExportDocument.setResearchOutput(
                thesisResearchOutputRepository.findResearchOutputIdsForThesis(document.getId()));
        }
    }

    private static void addDocumentFileInformation(ExportDocument commonExportDocument,
                                                   DocumentFile file) {
        var exportDocumentFile = new ExportDocumentFile();
        exportDocumentFile.setAccessRights(file.getAccessRights());
        exportDocumentFile.setLicense(file.getLicense());
        exportDocumentFile.setType(file.getResourceType());
        exportDocumentFile.setCreationDate(file.getCreateDate());
        exportDocumentFile.setLastUpdated(file.getLastModification());

        commonExportDocument.getDocumentFiles().add(exportDocumentFile);
    }

    private static ExportContribution createExportContribution(
        PersonDocumentContribution contribution) {
        var exportContribution = new ExportContribution();
        exportContribution.setOrderNumber(contribution.getOrderNumber());
        exportContribution.setDisplayName(
            contribution.getAffiliationStatement().getDisplayPersonName().toString());
        if (Objects.nonNull(contribution.getPerson())) {
            var person = new ExportPerson();
            person.setDatabaseId(contribution.getPerson().getId());
            person.setOldIds(contribution.getPerson().getOldIds());
            person.setName(new ExportPersonName(contribution.getPerson().getName().getFirstname(),
                contribution.getPerson().getName().getOtherName(),
                contribution.getPerson().getName().getLastname()));
            exportContribution.setPerson(person);
        }

        if (Objects.nonNull(contribution.getInstitutions())) {
            exportContribution.setDeclaredContributions(
                contribution.getInstitutions().stream().map(BaseEntity::getId).toList());
        }

        return exportContribution;
    }

    private static Set<Integer> getRelatedInstitutions(Document document) {
        var relations = new HashSet<Integer>();
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            document.getId()).ifPresent(documentIndex -> {
            relations.addAll(documentIndex.getOrganisationUnitIds());
        });

        if (document instanceof Proceedings) {
            relations.addAll(
                documentRepository.findInstitutionIdsByProceedingsIdAndAuthorContribution(
                    document.getId()));
        } else if (document instanceof Monograph) {
            relations.addAll(
                documentRepository.findInstitutionIdsByMonographIdAndAuthorContribution(
                    document.getId()));
        }

        return relations;
    }

    public static Publication toOpenaireModel(ExportDocument exportDocument,
                                              boolean supportLegacyIdentifiers) {
        var openairePublication = new Publication();

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            openairePublication.setOldId("Publications/" + legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        } else {
            openairePublication.setOldId(
                "Publications/" + IdentifierUtil.identifierPrefix + exportDocument.getDatabaseId());
        }

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

        if (Objects.nonNull(exportDocument.getUris()) && !exportDocument.getUris().isEmpty()) {
            openairePublication.setUrl(new ArrayList<>());
            openairePublication.getUrl().add(exportDocument.getUris().getFirst());
        }

        openairePublication.setDoi(exportDocument.getDoi());
        openairePublication.setScpNumber(exportDocument.getScopus());
        openairePublication.setIssn(new ArrayList<>());


        if (Objects.nonNull(exportDocument.getEIssn()) && !exportDocument.getEIssn().isBlank()) {
            openairePublication.getIssn().add(exportDocument.getEIssn());
        }

        if (Objects.nonNull(exportDocument.getPrintIssn()) &&
            !exportDocument.getPrintIssn().isBlank()) {
            openairePublication.getIssn().add(exportDocument.getPrintIssn());
        }

        openairePublication.setIsbn(
            Objects.nonNull(exportDocument.getEIsbn()) && !exportDocument.getEIsbn().isBlank() ?
                exportDocument.getEIsbn() : exportDocument.getPrintIsbn());
        openairePublication.setEdition(exportDocument.getEdition());

        openairePublication.setAccess(
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        if (Objects.nonNull(exportDocument.getJournal())) {
            openairePublication.setPublishedIn(new PublishedIn(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getJournal(),
                    supportLegacyIdentifiers)));
        }

        if (Objects.nonNull(exportDocument.getProceedings())) {
            var partOf = new PartOf();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                exportDocument.getProceedings().getTitle().stream(),
                Function.identity(),
                partOf::setDisplayName
            );
            partOf.setPublication(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getProceedings(),
                    supportLegacyIdentifiers));
            openairePublication.setPartOf(partOf);
        } else if (Objects.nonNull(exportDocument.getMonograph())) {
            var partOf = new PartOf();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                exportDocument.getMonograph().getTitle().stream(),
                Function.identity(),
                partOf::setDisplayName
            );
            partOf.setPublication(
                ExportDocumentConverter.toOpenaireModel(exportDocument.getMonograph(),
                    supportLegacyIdentifiers));
            openairePublication.setPartOf(partOf);
        }

        exportDocument.getDescription().stream()
            .min(Comparator.comparingInt(ExportMultilingualContent::getPriority))
            .map(mc -> List.of(new MultilingualContent(mc.getLanguageTag(), mc.getContent())))
            .ifPresent(openairePublication::set_abstract);

        openairePublication.set_abstract(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getDescription()));

        openairePublication.setKeywords(new ArrayList<>());
        exportDocument.getKeywords().forEach(mc -> {
            openairePublication.getKeywords()
                .add(new MultilingualContent(mc.getLanguageTag(),
                    mc.getContent().replace("\n", ";")));
        });

        openairePublication.setAuthors(new ArrayList<>());
        exportDocument.getAuthors()
            .forEach(contribution -> {
                var personAttributes = new PersonAttributes();
                personAttributes.setDisplayName(contribution.getDisplayName());

                if (Objects.nonNull(contribution.getPerson())) {
                    personAttributes.setPerson(
                        ExportPersonConverter.toOpenaireModel(contribution.getPerson(),
                            supportLegacyIdentifiers));
                }

                openairePublication.getAuthors().add(personAttributes);
            });

        openairePublication.setEditors(new ArrayList<>());
        exportDocument.getEditors()
            .forEach(contribution -> {
                var personAttributes = new PersonAttributes();
                personAttributes.setDisplayName(contribution.getDisplayName());

                if (Objects.nonNull(contribution.getPerson())) {
                    personAttributes.setPerson(
                        ExportPersonConverter.toOpenaireModel(contribution.getPerson(),
                            supportLegacyIdentifiers));
                }

                openairePublication.getEditors().add(personAttributes);
            });

        openairePublication.setInstitutions(new ArrayList<>());
        exportDocument.getPublishers().forEach(publisher -> {
            var openairePublisher = new Institution();
            ExportMultilingualContentConverter.setFieldFromPriorityContent(
                publisher.getName().stream(),
                Function.identity(),
                openairePublisher::setDisplayName
            );
            openairePublication.getInstitutions().add(openairePublisher);
        });

        return openairePublication;
    }

    public static DC toDCModel(ExportDocument exportDocument, boolean supportLegacyIdentifiers) {
        var dcPublication = new DC();

        setDCCommonFields(exportDocument, dcPublication, supportLegacyIdentifiers);

        return dcPublication;
    }

    public static ETDMSThesis toETDMSModel(ExportDocument exportDocument,
                                           boolean supportLegacyIdentifiers) {
        var thesisType = new ThesisType();

        setDCCommonFields(exportDocument, thesisType, supportLegacyIdentifiers);

        if (exportDocument.getType().equals(ExportPublicationType.THESIS)) {
            var degree = new Degree();

            if (Objects.nonNull(exportDocument.getThesisGrantor())) {
                addContentToList(
                    exportDocument.getThesisGrantor().getName(),
                    ExportMultilingualContent::getContent,
                    content -> degree.getGrantor().add(content)
                );
            } else {
                addContentToList(
                    exportDocument.getExternalThesisGrantorName(),
                    ExportMultilingualContent::getContent,
                    content -> degree.getGrantor().add(content)
                );
            }

            degree.setLevel(
                new LevelType(String.valueOf(exportDocument.getThesisType().ordinal() % 3)));
            degree.getName().add(exportDocument.getThesisType().name());
            thesisType.setDegree(degree);
        }

        var thesis = new ETDMSThesis();
        thesis.setThesisType(thesisType);
        return thesis;
    }

    public static Marc21 toMARC21Model(ExportDocument exportDocument,
                                       boolean supportLegacyIdentifiers) {
        Marc21 marc21 = new Marc21();
        marc21.setLeader("ca a2 n");

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            marc21.getControlFields()
                .add(new ControlField("001", legacyIdentifierPrefix +
                    exportDocument.getOldIds().stream().findFirst().get()));
        } else {
            marc21.getControlFields()
                .add(new ControlField("001", identifierPrefix + exportDocument.getId()));
        }

        if (Objects.nonNull(exportDocument.getDoi())) {
            marc21.getDataFields()
                .add(createDataField("024", "7", " ", "a", exportDocument.getDoi()));
        }

        clientLanguages.forEach(lang ->
            marc21.getDataFields().add(createDataField("856", "4", "1", "u",
                baseFrontendUrl + lang + "/scientific-results/thesis/" + exportDocument.getId()))
        );

        addContentToMarc21(marc21, "245", "1", "0", exportDocument.getTitle(),
            ExportMultilingualContent::getContent, "a");

        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.AUTHOR, "100", "1",
            " ");

        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.EDITOR, "700", "1",
            " ");
        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.ADVISOR, "700",
            "1", " ");
        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.BOARD_MEMBER,
            "700", "1", " ");

        addContentToMarc21(marc21, "520", " ", " ", exportDocument.getDescription(),
            ExportMultilingualContent::getContent, "a");

        addContentToMarc21(marc21, "650", " ", "7", exportDocument.getKeywords(),
            ExportMultilingualContent::getContent, "a");

        if (Objects.nonNull(exportDocument.getLanguageTags())) {
            exportDocument.getLanguageTags().forEach(tag -> {
                marc21.getDataFields().add(createDataField("041", " ", " ", "a", tag));
            });
        }

        if (Objects.nonNull(exportDocument.getPublishers())) {
            exportDocument.getPublishers().forEach(publisher -> {
                addContentToMarc21(marc21, "260", " ", " ", publisher.getName(),
                    ExportMultilingualContent::getContent, "b");
            });
        }

        addContentToMarc21(marc21, "856", "4", "1",
            new ArrayList<>(exportDocument.getFileFormats()), Function.identity(), "q");

        var isOpenAccess = exportDocument.getOpenAccess();

        String accessRights = isOpenAccess ?
            "info:eu-repo/semantics/openAccess" :
            "info:eu-repo/semantics/metadataOnlyAccess";

        marc21.getDataFields().add(createDataField("506", " ", " ", "a", accessRights));
        marc21.getDataFields().add(createDataField("540", " ", " ", "a",
            "http://creativecommons.org/publicdomain/zero/1.0/"));

        return marc21;
    }

    private static void addContributorsToMarc21(Marc21 marc21, ExportDocument exportDocument,
                                                DocumentContributionType type,
                                                String tag, String ind1, String ind2) {
        List<ExportContribution> contributions;

        contributions = switch (type) {
            case AUTHOR -> exportDocument.getAuthors();
            case EDITOR -> exportDocument.getEditors();
            case ADVISOR -> exportDocument.getAdvisors();
            case BOARD_MEMBER -> exportDocument.getBoardMembers();
            case REVIEWER -> null;
        };

        if (Objects.isNull(contributions)) {
            return; // should never happen as we don't call using REVIEWER type, left just in case
        }

        addContentToMarc21(
            marc21, tag, ind1, ind2,
            contributions.stream()
                .map(ExportContribution::getDisplayName)
                .collect(Collectors.toList()),
            Object::toString, "a"
        );
    }

    private static <T> void addContentToMarc21(Marc21 marc21, String tag, String ind1, String ind2,
                                               List<T> content, Function<T, String> extractor,
                                               String subfieldCode) {
        content.stream().map(extractor).forEach(value ->
            marc21.getDataFields().add(createDataField(tag, ind1, ind2, subfieldCode, value))
        );
    }

    private static DataField createDataField(String tag, String ind1, String ind2,
                                             String subfieldCode, String value) {
        var dataField = new DataField(tag, ind1, ind2, new ArrayList<>());
        dataField.getSubFields().add(new SubField(subfieldCode, value));
        return dataField;
    }

    public static Dim toDIMModel(ExportDocument exportDocument, boolean supportLegacyIdentifiers) {
        var dimPublication = new Dim();

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            dimPublication.getFields().add(
                new DimField("dc", "identifier", "internal", null, null, null,
                    legacyIdentifierPrefix +
                        exportDocument.getOldIds().stream().findFirst().get()));
        } else {
            dimPublication.getFields().add(
                new DimField("dc", "identifier", "internal", null, null, null,
                    identifierPrefix + exportDocument.getDatabaseId()));
        }

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
        } else if (Objects.nonNull(exportDocument.getExternalThesisGrantorName())) {
            exportDocument.getExternalThesisGrantorName().forEach(mc -> {
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

        exportDocument.getDocumentFiles().stream().filter(file -> file.getType().equals(
                ResourceType.OFFICIAL_PUBLICATION) && Objects.nonNull(file.getLicense())).findFirst()
            .ifPresent(officialPublication -> {
                var license = switch (officialPublication.getLicense()) {
                    case BY -> "Attribution";
                    case BY_SA -> "Attribution-ShareAlike";
                    case BY_NC -> "Attribution-NonCommercial";
                    case BY_NC_SA -> "Attribution-NonCommercial-ShareAlike";
                    case BY_ND -> "Attribution-NoDerivs";
                    case BY_NC_ND -> "Attribution-NonCommercial-NoDerivs";
                    case CC0 -> "PublicDomain";
                };

                dimPublication.getFields().add(
                    new DimField("dc", "rights", null,
                        null, null, null, license)
                );
            });

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

    private static void setDCCommonFields(ExportDocument exportDocument, DC dcPublication,
                                          boolean supportLegacyIdentifiers) {
        dcPublication.getDate().add(exportDocument.getDocumentDate());
        dcPublication.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            dcPublication.getIdentifier().add(legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        }

        dcPublication.getIdentifier().add(identifierPrefix + exportDocument.getDatabaseId());

        dcPublication.getType().add(exportDocument.getType().name());

        clientLanguages.forEach(lang -> {
            dcPublication.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-results/" +
                    getConcreteEntityPath(exportDocument.getType()) + "/" +
                    exportDocument.getDatabaseId());
        });

        if (StringUtil.valueExists(exportDocument.getDoi())) {
            dcPublication.getIdentifier().add("DOI:" + exportDocument.getDoi());
        }

        if (StringUtil.valueExists(exportDocument.getScopus())) {
            dcPublication.getIdentifier().add("SCOPUS:" + exportDocument.getScopus());
        }

        if (StringUtil.valueExists(exportDocument.getOpenAlex())) {
            dcPublication.getIdentifier().add("OPENALEX:" + exportDocument.getOpenAlex());
        }

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPublication.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcPublication.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getEditors(),
            ExportContribution::getDisplayName,
            contribution -> Objects.requireNonNullElse(contribution.getPerson().getOrcid(), ""),
            (content, orcid) -> dcPublication.getContributor().add(
                new Contributor(content, "editor",
                    (orcid.isBlank() ? "" : ("https://orcid.org/" + orcid))))
        );

        addContentToList(
            exportDocument.getAdvisors(),
            ExportContribution::getDisplayName,
            contribution -> Objects.requireNonNullElse(contribution.getPerson().getOrcid(), ""),
            (content, orcid) -> dcPublication.getContributor().add(
                new Contributor(content, "advisor",
                    (orcid.isBlank() ? "" : ("https://orcid.org/" + orcid))))
        );

        addContentToList(
            exportDocument.getBoardMembers(),
            ExportContribution::getDisplayName,
            contribution -> Objects.requireNonNullElse(contribution.getPerson().getOrcid(), ""),
            (content, orcid) -> dcPublication.getContributor().add(
                new Contributor(content, "board_member",
                    (orcid.isBlank() ? "" : ("https://orcid.org/" + orcid))))
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPublication.getDescription()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPublication.getSubject()
                .add(new DCMultilingualContent(content.replace("\n", "; "), languageTag))
        );

        addContentToList(
            exportDocument.getLanguageTags(),
            Function.identity(),
            content -> dcPublication.getLanguage().add(content.toLowerCase())
        );

        exportDocument.getPublishers().forEach(publisher -> {
            publisher.getName().forEach(name -> {
                dcPublication.getPublisher().add(new DCMultilingualContent(name.getContent(),
                    name.getLanguageTag().toLowerCase()));
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
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcPublication.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");
    }
}
