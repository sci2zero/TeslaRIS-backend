package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import jakarta.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.document.DocumentPublicationConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ProceedingsReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
@Traceable
public class DocumentPublicationServiceImpl extends JPAServiceImpl<Document>
    implements DocumentPublicationService {

    protected final MultilingualContentService multilingualContentService;

    protected final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    protected final SearchService<DocumentPublicationIndex> searchService;

    protected final OrganisationUnitService organisationUnitService;

    protected final DocumentRepository documentRepository;

    protected final DocumentFileService documentFileService;

    private final PersonContributionService personContributionService;

    private final ExpressionTransformer expressionTransformer;

    private final EventService eventService;

    private final CommissionRepository commissionRepository;

    private final SearchFieldsLoader searchFieldsLoader;

    private final OrganisationUnitTrustConfigurationService
        organisationUnitTrustConfigurationService;

    private final Pattern doiPattern =
        Pattern.compile("\"^10\\\\.\\\\d{4,9}\\\\/[-,._;()/:A-Z0-9]+$\"", Pattern.CASE_INSENSITIVE);

    @Value("${document.approved_by_default}")
    protected Boolean documentApprovedByDefault;


    @Override
    protected JpaRepository<Document, Integer> getEntityRepository() {
        return documentRepository;
    }

    @Override
    public DocumentDTO readDocumentPublication(Integer documentId) {
        return DocumentPublicationConverter.toDTO(findOne(documentId));
    }

    @Override
    @Deprecated(forRemoval = true)
    public Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document with given id does not exist."));
    }

    @Override
    @Nullable
    public Document findDocumentByOldId(Integer documentOldId) {
        var documentId = documentRepository.findDocumentByOldIdsContains(documentOldId);
        return documentId.map(this::findOne).orElse(null);
    }

    @Override
    public Page<DocumentPublicationIndex> findResearcherPublications(Integer authorId,
                                                                     List<Integer> ignore,
                                                                     Pageable pageable) {
        return documentPublicationIndexRepository.findByAuthorIdsAndDatabaseIdNotIn(
            authorId, ignore, pageable);
    }

    @Override
    public List<Integer> getResearchOutputIdsForDocument(Integer documentId) {
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                documentId).orElseThrow(
                () -> new NotFoundException("Document with ID " + documentId + " does not exist."))
            .getResearchOutputIds();
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForPublisher(Integer publisherId,
                                                                       Pageable pageable) {
        return documentPublicationIndexRepository.findByPublisherId(publisherId, pageable);
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForOrganisationUnit(
        Integer organisationUnitId, List<String> tokens, List<DocumentPublicationType> allowedTypes,
        Pageable pageable) {
        var allOUIdsFromSubHierarchy =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        if (Objects.isNull(tokens)) {
            tokens = List.of("*");
        }

        var simpleSearchQuery = buildSimpleSearchQuery(tokens, null, null, allowedTypes);

        var institutionFilter = TermsQuery.of(t -> t
            .field("organisation_unit_ids")
            .terms(v -> v.value(
                allOUIdsFromSubHierarchy.stream()
                    .map(String::valueOf)
                    .map(FieldValue::of)
                    .toList()))
        )._toQuery();

        var combinedQuery = BoolQuery.of(bq -> bq
            .must(simpleSearchQuery)
            .must(institutionFilter)
        )._toQuery();

        return searchService.runQuery(combinedQuery, pageable, DocumentPublicationIndex.class,
            "document_publication");
    }

    @Override
    public Long getPublicationCount() {
        if (SessionTrackingUtil.isUserLoggedIn()) {
            return documentPublicationIndexRepository.countPublications();
        }

        return documentPublicationIndexRepository.countApprovedPublications();
    }

    @Override
    public void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved) {
        var documentToUpdate = findOne(documentId);

        if (documentToUpdate.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            documentToUpdate.setApproveStatus(
                isApproved ? ApproveStatus.APPROVED : ApproveStatus.DECLINED);
        }

        documentRepository.save(documentToUpdate);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentFileResponseDTO addDocumentFile(Integer documentId, DocumentFileDTO file,
                                                   Boolean isProof) {
        var document = findOne(documentId);
        var documentFile = documentFileService.saveNewPublicationDocument(file, !isProof, document,
            !shouldFileItemsBeValidated(document));
        if (isProof) {
            document.getProofs().add(documentFile);
        } else {
            document.getFileItems().add(documentFile);
        }

        if (!documentFile.getIsVerifiedData()) {
            document.setAreFilesValid(false);
        }

        documentRepository.save(document);

        if (!isProof) {
            indexDocumentFilesContent(document,
                findDocumentPublicationIndexByDatabaseId(documentId));
        }

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public void deleteDocumentFile(Integer documentId, Integer documentFileId) {
        var document = findOne(documentId);
        var documentFile = documentFileService.findOne(documentFileId);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());

        var isProof =
            document.getProofs().stream().anyMatch((proof) -> proof.getId().equals(documentFileId));

        document.getFileItems().remove(documentFile);
        document.getProofs().remove(documentFile);

        document.setAreFilesValid(document.getFileItems().stream()
            .noneMatch(file -> file.getIsVerifiedData().equals(false)) &&
            document.getProofs().stream()
                .noneMatch(file -> file.getIsVerifiedData().equals(false)));

        if (!isProof) {
            indexDocumentFilesContent(document,
                findDocumentPublicationIndexByDatabaseId(documentId));
        }
    }

    @Override
    public void deleteDocumentPublication(Integer documentId) {
        var document = findOne(documentId);
        documentRepository.delete(document);

        var index =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);
        index.ifPresent(documentPublicationIndexRepository::delete);

        // TODO: should we delete all document file indexes as well
    }

    @Override
    public List<Integer> getContributorIds(Integer publicationId) {
        return findOne(publicationId).getContributors().stream().map(contribution -> {
                if (Objects.nonNull(contribution.getPerson())) {
                    return contribution.getPerson().getId();
                }
                return -1;
            })
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void indexCommonFields(Document document, DocumentPublicationIndex index) {
        clearCommonIndexFields(index);

        setBasicMetadata(document, index);
        setContributors(document, index);
        setAdditionalMetadata(document, index);

        index.setIsApproved(Objects.nonNull(document.getApproveStatus()) &&
            document.getApproveStatus().equals(ApproveStatus.APPROVED));
        index.setAreFilesValid(document.getAreFilesValid());
    }

    private void setBasicMetadata(Document document, DocumentPublicationIndex index) {
        index.setLastEdited(Objects.nonNull(document.getLastModification())
            ? document.getLastModification()
            : new Date());
        index.setDatabaseId(document.getId());
        index.setYear(parseYear(document.getDocumentDate()));
        indexTitle(document, index);
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setDoi(document.getDoi());
        index.setScopusId(document.getScopusId());
        index.setOpenAlexId(document.getOpenAlexId());
        index.setIsOpenAccess(documentRepository.isDocumentPubliclyAvailable(document.getId()));
        indexDescription(document, index);
        indexKeywords(document, index);
        indexDocumentFilesContent(document, index);

        if (Objects.nonNull(document.getEvent())) {
            index.setEventId(document.getEvent().getId());
        }

        index.setWordcloudTokensSr(
            StringUtil.extractKeywords(index.getTitleSr(), index.getDescriptionSr(),
                index.getKeywordsSr()));
        index.setWordcloudTokensOther(
            StringUtil.extractKeywords(index.getTitleOther(), index.getDescriptionOther(),
                index.getKeywordsOther()));
    }

    private void setContributors(Document document, DocumentPublicationIndex index) {
        var organisationUnitIds = new HashSet<Integer>();

        document.getContributors().stream()
            .sorted(Comparator.comparingInt(PersonContribution::getOrderNumber))
            .forEach(contribution -> processContribution(contribution, index, organisationUnitIds));

        index.setOrganisationUnitIds(new ArrayList<>(organisationUnitIds.stream().toList()));
        index.setAuthorNamesSortable(index.getAuthorNames());
    }

    private void processContribution(PersonDocumentContribution contribution,
                                     DocumentPublicationIndex index,
                                     Set<Integer> organisationUnitIds) {
        var personExists = Objects.nonNull(contribution.getPerson());
        var contributorName =
            contribution.getAffiliationStatement().getDisplayPersonName().toString();

        organisationUnitIds.addAll(contribution.getInstitutions().stream()
            .map(BaseEntity::getId)
            .toList());

        switch (contribution.getContributionType()) {
            case AUTHOR ->
                handleAuthorContribution(contribution, index, contributorName, personExists);
            case EDITOR -> handleGenericContribution(contribution, index::getEditorIds,
                index::setEditorNames, contributorName, personExists);
            case ADVISOR -> handleGenericContribution(contribution, index::getAdvisorIds,
                index::setAdvisorNames, contributorName, personExists);
            case REVIEWER -> handleGenericContribution(contribution, index::getReviewerIds,
                index::setReviewerNames, contributorName, personExists);
            case BOARD_MEMBER ->
                handleBoardMember(contribution, index, contributorName, personExists);
        }
    }

    private void handleAuthorContribution(PersonDocumentContribution contribution,
                                          DocumentPublicationIndex index,
                                          String contributorName, boolean personExists) {
        if (contribution.getIsCorrespondingContributor()) {
            contributorName += "*";
        }

        index.getAuthorIds().add(personExists ? contribution.getPerson().getId() : -1);
        index.setAuthorNames(StringUtil.removeLeadingColonSpace(
            index.getAuthorNames() + "; " + contributorName));
    }

    private void handleGenericContribution(PersonContribution contribution,
                                           Supplier<List<Integer>> idGetter,
                                           Consumer<String> nameSetter,
                                           String contributorName, boolean personExists) {
        idGetter.get().add(personExists ? contribution.getPerson().getId() : -1);
        nameSetter.accept(StringUtil.removeLeadingColonSpace("; " + contributorName));
    }

    private void handleBoardMember(PersonDocumentContribution contribution,
                                   DocumentPublicationIndex index,
                                   String contributorName, boolean personExists) {
        index.getBoardMemberIds().add(personExists ? contribution.getPerson().getId() : -1);

        if (contribution.getIsBoardPresident()) {
            if (personExists) {
                index.setBoardPresidentId(contribution.getPerson().getId());
            }
            index.setBoardPresidentName(contributorName);
        }

        index.setBoardMemberNames(StringUtil.removeLeadingColonSpace(
            index.getBoardMemberNames() + "; " + contributorName));
    }

    private void setAdditionalMetadata(Document document, DocumentPublicationIndex index) {
        index.setAssessedBy(
            commissionRepository.findCommissionsThatAssessedDocument(document.getId()));
    }

    @Override
    public void reindexDocumentVolatileInformation(Integer documentId) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(documentIndex -> {
                documentIndex.setAssessedBy(
                    commissionRepository.findCommissionsThatAssessedDocument(documentId));
                documentPublicationIndexRepository.save(documentIndex);
            });
    }

    @Override
    public DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId) {
        var fallbackDocument = new DocumentPublicationIndex();
        fallbackDocument.setDatabaseId(documentId);
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            documentId).orElse(fallbackDocument);
    }

    private void indexDocumentFilesContent(Document document, DocumentPublicationIndex index) {
        index.setFullTextSr("");
        index.setFullTextOther("");
        index.setIsOpenAccess(false);
        index.setAreFilesValid(document.getAreFilesValid());

        document.getFileItems().forEach(documentFile -> {
            index.setIsOpenAccess(documentRepository.isDocumentPubliclyAvailable(document.getId()));

            if (!documentFile.getMimeType().contains("pdf")) {
                return;
            }

            var file = documentFileService.findDocumentFileIndexByDatabaseId(documentFile.getId());
            index.setFullTextSr(index.getFullTextSr() + file.getPdfTextSr());
            index.setFullTextOther(index.getFullTextOther() + " " + file.getPdfTextOther());
        });

        documentPublicationIndexRepository.save(index);
    }

    private void indexTitle(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getTitle(), true);
        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getSubTitle(), false);

        StringUtil.removeTrailingDelimiters(contentSr, contentOther);
        index.setTitleSr(!contentSr.isEmpty() ? contentSr.toString() : contentOther.toString());
        index.setTitleOther(
            !contentOther.isEmpty() ? contentOther.toString() : contentSr.toString());
    }

    private void indexDescription(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStringsFromHTMLMC(contentSr, contentOther,
            document.getDescription(), false);

        StringUtil.removeTrailingDelimiters(contentSr, contentOther);
        index.setDescriptionSr(
            !contentSr.isEmpty() ? contentSr.toString() : contentOther.toString());
        index.setDescriptionOther(
            !contentOther.isEmpty() ? contentOther.toString() : contentSr.toString());
    }

    private void indexKeywords(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getKeywords(), false);

        StringUtil.removeTrailingDelimiters(contentSr, contentOther);
        index.setKeywordsSr(
            !contentSr.isEmpty() ? contentSr.toString() : contentOther.toString());
        index.setKeywordsOther(
            !contentOther.isEmpty() ? contentOther.toString() : contentSr.toString());
    }

    private int parseYear(String dateString) {
        if (Objects.isNull(dateString)) {
            return -1;
        }

        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy"), // Year only
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy.")
        };

        for (var formatter : formatters) {
            try {
                TemporalAccessor parsed = formatter.parse(dateString);

                if (parsed.isSupported(ChronoField.YEAR)) {
                    return parsed.get(ChronoField.YEAR);
                }
            } catch (DateTimeParseException e) {
                // Parsing failed, try the next formatter
            }
        }

        return -1;
    }

    protected void setCommonFields(Document document, DocumentDTO documentDTO) {
        document.setTitle(
            multilingualContentService.getMultilingualContent(documentDTO.getTitle()));
        document.setSubTitle(
            multilingualContentService.getMultilingualContent(documentDTO.getSubTitle()));
        document.setDescription(
            multilingualContentService.getMultilingualContent(documentDTO.getDescription()));
        document.setKeywords(
            multilingualContentService.getMultilingualContent(documentDTO.getKeywords()));

        personContributionService.setPersonDocumentContributionsForDocument(document, documentDTO);

        if (Objects.nonNull(documentDTO.getOldId())) {
            document.getOldIds().add(documentDTO.getOldId());
        }

        document.setDocumentDate(documentDTO.getDocumentDate());

        IdentifierUtil.setUris(document.getUris(), documentDTO.getUris());
        setCommonIdentifiers(document, documentDTO);

        if (Objects.nonNull(documentDTO.getEventId())) {
            var event = eventService.findOne(documentDTO.getEventId());

            if (event.getSerialEvent()) {
                throw new ProceedingsReferenceConstraintViolationException(
                    "Proceedings cannot be bound to serial event.");
            }

            document.setEvent(event);
        }

        if (!documentApprovedByDefault) {
            document.setIsMetadataValid(false);
        } else {
            document.setIsMetadataValid(!shouldMetadataBeValidated(document));
        }

        document.setApproveStatus(
            document.getIsMetadataValid() ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);
    }

    private void setCommonIdentifiers(Document document, DocumentDTO documentDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            documentDTO.getDoi(),
            document.getId(),
            "^10\\.\\d{4,9}\\/[-,._;()/:A-Z0-9]+$",
            documentRepository::existsByDoi,
            document::setDoi,
            "doiFormatError",
            "doiExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            documentDTO.getScopusId(),
            document.getId(),
            "^\\d{6,12}$",
            documentRepository::existsByScopusId,
            document::setScopusId,
            "scopusIdFormatError",
            "scopusIdExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            documentDTO.getOpenAlexId(),
            document.getId(),
            "^W\\d{4,10}$",
            documentRepository::existsByOpenAlexId,
            document::setOpenAlexId,
            "openAlexIdFormatError",
            "openAlexIdExistsError"
        );
    }

    @Override
    public boolean isIdentifierInUse(String identifier, Integer documentPublicationId) {
        return documentRepository.existsByDoi(identifier, documentPublicationId) ||
            documentRepository.existsByScopusId(identifier, documentPublicationId) ||
            documentRepository.existsByOpenAlexId(identifier, documentPublicationId);
    }

    @Override
    public boolean isDoiInUse(String doi) {
        return documentRepository.existsByDoi(doi, null);
    }

    @Override
    public Pair<Long, Long> getDocumentCountsBelongingToInstitution(Integer institutionId) {
        return new Pair<>(documentPublicationIndexRepository.countAssessable(),
            documentPublicationIndexRepository.countAssessableByOrganisationUnitIds(institutionId));
    }

    @Override
    public Pair<Long, Long> getAssessedDocumentCountsForCommission(Integer institutionId,
                                                                   Integer commissionId) {
        return new Pair<>(documentPublicationIndexRepository.countByAssessedBy(commissionId),
            documentPublicationIndexRepository.countByOrganisationUnitIdsAndAssessedBy(
                institutionId, commissionId));
    }

    @Override
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields) {
        return searchFieldsLoader.getSearchFields("documentSearchFieldConfiguration.json",
            onlyExportFields);
    }

    @Override
    public List<Pair<String, Long>> getWordCloudForSingleDocument(Integer documentId,
                                                                  boolean foreignLanguage) {
        var document =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
                .orElseThrow(() -> new NotFoundException(
                    "Document with ID " + documentId + " does not exist."));
        var terms =
            foreignLanguage ? document.getWordcloudTokensOther() : document.getWordcloudTokensSr();

        Map<String, Long> result = terms.parallelStream().
            collect(Collectors.toConcurrentMap(
                w -> w, w -> 1L, Long::sum));

        return result.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(30)
            .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Document> findDocumentByCommonIdentifier(String doi, String openAlexId,
                                                             String scopusId) {
        if (Objects.isNull(doi) || doi.isBlank()) {
            doi = "NOT_PRESENT";
        } else {
            doi = doi.replace("https://doi.org/", "");
        }

        if (Objects.isNull(openAlexId) || openAlexId.isBlank()) {
            openAlexId = "NOT_PRESENT";
        } else {
            openAlexId = openAlexId.replace("https://openalex.org/", "");
        }

        if (Objects.isNull(scopusId) || scopusId.isBlank()) {
            scopusId = "NOT_PRESENT";
        } else {
            scopusId = scopusId.replace("SCOPUS:", "");
        }

        return documentRepository.findByOpenAlexIdOrDoiOrScopusId(openAlexId, doi, scopusId);
    }

    protected void clearCommonFields(Document publication) {
        publication.getTitle().clear();
        publication.getSubTitle().clear();
        publication.getDescription().clear();
        publication.getKeywords().clear();

        publication.getContributors().forEach(
            contribution -> personContributionService.deleteContribution(contribution.getId()));
        publication.getContributors().clear();
    }

    private void clearCommonIndexFields(DocumentPublicationIndex index) {
        index.setAuthorNames("");
        index.setEditorNames("");
        index.setReviewerNames("");
        index.setAdvisorNames("");

        index.getAuthorIds().clear();
        index.getEditorIds().clear();
        index.getReviewerIds().clear();
        index.getAdvisorIds().clear();
    }

    protected void deleteProofsAndFileItems(Document publicationToDelete) {
        publicationToDelete.getProofs()
            .forEach(proof -> documentFileService.deleteDocumentFile(proof.getServerFilename()));
        publicationToDelete.getFileItems().forEach(
            fileItem -> documentFileService.deleteDocumentFile(fileItem.getServerFilename()));
    }

    @Override
    public Page<DocumentPublicationIndex> searchDocumentPublications(List<String> tokens,
                                                                     Pageable pageable,
                                                                     SearchRequestType type,
                                                                     Integer institutionId,
                                                                     Integer commissionId,
                                                                     List<DocumentPublicationType> allowedTypes) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(
                buildSimpleSearchQuery(tokens, institutionId, commissionId, allowedTypes),
                pageable,
                DocumentPublicationIndex.class, "document_publication");
        }

        return searchService.runQuery(
            buildAdvancedSearchQuery(tokens, institutionId, commissionId, allowedTypes), pageable,
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    public Page<DocumentPublicationIndex> findDocumentDuplicates(List<String> titles,
                                                                 String doi,
                                                                 String scopusId,
                                                                 String openAlexId) {
        var query = buildDeduplicationSearchQuery(titles, doi, scopusId, openAlexId);
        return searchService.runQuery(query,
            Pageable.ofSize(5),
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    public Page<DocumentPublicationIndex> findNonAffiliatedDocuments(Integer organisationUnitId,
                                                                     Integer personId,
                                                                     Pageable pageable) {
        var nonAffiliatedDocumentIds =
            personContributionService.getIdsOfNonRelatedDocuments(organisationUnitId, personId);
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(
            nonAffiliatedDocumentIds, pageable);
    }

    @Override
    public void massAssignContributionInstitution(Integer organisationUnitId, Integer personId,
                                                  List<Integer> documentIds, Boolean deleteOthers) {
        documentIds.forEach(documentId -> {
            var document = findOne(documentId);
            var contributionToUpdate = document.getContributors().stream()
                .filter(contribution -> contribution.getPerson().getId().equals(personId))
                .findFirst();
            contributionToUpdate.ifPresent(contribution -> {
                if (deleteOthers) {
                    contribution.getInstitutions().clear();
                }

                contribution.getInstitutions()
                    .add(organisationUnitService.findOne(organisationUnitId));
            });

            save(document);
        });
    }

    @Override
    public void deleteIndexes() {
        documentPublicationIndexRepository.deleteAll();
    }

    @Override
    public void reorderDocumentContributions(Integer documentId, Integer contributionId,
                                             Integer oldContributionOrderNumber,
                                             Integer newContributionOrderNumber) {
        var document = findOne(documentId);
        var contributions = document.getContributors().stream()
            .map(contribution -> (PersonContribution) contribution).collect(
                Collectors.toSet());

        personContributionService.reorderContributions(contributions, contributionId,
            oldContributionOrderNumber, newContributionOrderNumber);

        document.getContributors().forEach(parentContribution -> {
            contributions.forEach(contribution -> {
                if (contribution.getId().equals(parentContribution.getId())) {
                    contribution.setOrderNumber(parentContribution.getOrderNumber());
                }
            });
        });

        save(document);

        var index =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);
        index.ifPresent(documentPublicationIndex -> {
            indexCommonFields(document, documentPublicationIndex);
            documentPublicationIndexRepository.save(documentPublicationIndex);
        });
    }

    @Override
    public void unbindResearcherFromContribution(Integer personId, Integer documentId) {
        var contribution =
            personContributionService.findContributionForResearcherAndDocument(personId,
                documentId);

        if (Objects.isNull(contribution)) {
            return;
        }

        contribution.setPerson(null);
        contribution.getInstitutions().clear();
        personContributionService.save(contribution);

        var document = documentRepository.findById(documentId);

        var indexOptional =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (document.isPresent() && indexOptional.isPresent()) {
            if (document.get() instanceof Thesis) {
                throw new ThesisException("You can't unbind yourself from thesis.");
            }

            var index = indexOptional.get();
            indexCommonFields(document.get(), index);
            documentPublicationIndexRepository.save(index);
        }
    }

    private Query buildDeduplicationSearchQuery(List<String> titles, String doi, String scopusId,
                                                String openAlexId) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    titles.forEach(title -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_sr").query(title)));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_other").query(title)));
                    });

                    if (Objects.nonNull(scopusId) && !scopusId.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("scopusId").query(scopusId)));
                    }

                    if (Objects.nonNull(doi) && !doi.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("doi").query(doi)));
                    }

                    if (Objects.nonNull(openAlexId) && !openAlexId.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("open_alex_id").query(openAlexId)));
                    }

                    return eq;
                });
                return bq;
            });
            return b;
        })))._toQuery();
    }

    private Query buildSimpleMetadataQuery(Integer institutionId,
                                           Integer commissionId,
                                           List<DocumentPublicationType> allowedTypes) {
        return BoolQuery.of(b -> {
            if (Objects.nonNull(institutionId) && institutionId > 0) {
                b.must(q -> q.term(t -> t.field("organisation_unit_ids").value(institutionId)));
            }

            if (Objects.nonNull(commissionId) && commissionId > 0) {
                b.mustNot(q -> q.term(t -> t.field("assessed_by").value(commissionId)));
            }

            if (Objects.nonNull(allowedTypes) && !allowedTypes.isEmpty()) {
                b.must(createTypeTermsQuery(allowedTypes));
            }

            if (!SessionTrackingUtil.isUserLoggedIn()) {
                b.must(q -> q.term(t -> t.field("is_approved").value(true)));
            }

            return b;
        })._toQuery();
    }

    private Query buildSimpleTokenQuery(List<String> tokens, int minShouldMatch) {
        return BoolQuery.of(eq -> {
            tokens.forEach(token -> {
                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    eq.must(mp -> mp.bool(m -> m
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_sr").query(token.replace("\\\"", ""))))
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_other").query(token.replace("\\\"", ""))))
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("author_names").query(token.replace("\\\"", ""))))
                    ));
                } else if (token.endsWith("\\*") || token.endsWith(".")) {
                    var wildcard = token.replace("\\*", "").replace(".", "");
                    eq.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("author_names")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true)))
                    ));
                } else {
                    var wildcard = token + "*";
                    eq.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard).caseInsensitive(true)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_sr").query(wildcard)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_other").query(wildcard)))
                        .should(sb -> sb.match(mq -> mq.field("author_names").query(token)))
                        .should(
                            sb -> sb.wildcard(mq -> mq.field("author_names")
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.term(mq -> mq.field("keywords_sr").value(token)))
                        .should(sb -> sb.term(mq -> mq.field("keywords_other").value(token)))
                    ));
                }

                eq
                    .should(sb -> sb.match(m -> m.field("description_sr").query(token).boost(0.7f)))
                    .should(
                        sb -> sb.match(m -> m.field("description_other").query(token).boost(0.7f)))
                    .should(sb -> sb.match(m -> m.field("full_text_sr").query(token).boost(0.7f)))
                    .should(
                        sb -> sb.match(m -> m.field("full_text_other").query(token).boost(0.7f)))
                    .should(sb -> sb.match(m -> m.field("editor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("reviewer_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("advisor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("type").query(token)))
                    .should(sb -> sb.match(m -> m.field("doi").query(token)));
            });

            return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
        })._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         Integer institutionId,
                                         Integer commissionId,
                                         List<DocumentPublicationType> allowedTypes) {
        int minShouldMatch;
        if (tokens.size() <= 2) {
            minShouldMatch = 1; // Allow partial match for very short queries
        } else {
            minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);
        }

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildSimpleMetadataQuery(institutionId, commissionId, allowedTypes));
            b.must(buildSimpleTokenQuery(tokens, minShouldMatch));
            b.mustNot(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.PROCEEDINGS.name())));
            return b;
        })))._toQuery();
    }

    public Query buildAdvancedSearchQuery(List<String> tokens,
                                          Integer institutionId,
                                          Integer commissionId,
                                          List<DocumentPublicationType> allowedTypes) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildSimpleMetadataQuery(institutionId, commissionId, allowedTypes));
            b.must(expressionTransformer.parseAdvancedQuery(tokens));
            b.mustNot(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.PROCEEDINGS.name())));

            return b;
        })))._toQuery();
    }

    private Query createTypeTermsQuery(List<DocumentPublicationType> values) {
        return TermsQuery.of(t -> t
            .field("type")
            .terms(v -> v.value(values.stream()
                .map(DocumentPublicationType::name)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
    }

    protected void sendNotifications(Document document) {
        var loggedInUser =
            (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        document.getContributors().forEach(contribution -> {
            if (Objects.isNull(contribution.getPerson())) {
                return;
            }

            var userOptional =
                personContributionService.getUserForContributor(contribution.getPerson().getId());
            if (userOptional.isPresent() &&
                !userOptional.get().getId().equals(loggedInUser.getId())) {
                var notificationValues = new HashMap<String, String>();
                notificationValues.put("title",
                    document.getTitle().stream().max(Comparator.comparingInt(
                        MultiLingualContent::getPriority)).get().getContent());
                notificationValues.put("contributionId", contribution.getId().toString());
                notificationValues.put("documentId", document.getId().toString());
                notificationValues.put("personId", contribution.getPerson().getId().toString());
                personContributionService.notifyContributor(
                    NotificationFactory.contructAddedToPublicationNotification(
                        notificationValues,
                        userOptional.get()));
            }
        });
    }

    protected void clearIndexWhenFailedRead(Integer documentId) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(documentPublicationIndexRepository::delete);
    }

    protected void checkForDocumentDate(DocumentDTO documentDTO) {
        if (Objects.isNull(documentDTO.getDocumentDate()) ||
            documentDTO.getDocumentDate().isBlank()) {
            throw new MissingDataException("This document requires a specified document date.");
        }
    }

    protected Boolean shouldMetadataBeValidated(Document document) {
        return shouldValidate(document, true);
    }

    protected Boolean shouldFileItemsBeValidated(Document document) {
        return shouldValidate(document, false);
    }

    private Boolean shouldValidate(Document document, boolean isMetadata) {
        var loggedInUser = SessionTrackingUtil.getLoggedInUser();

        if (Objects.isNull(loggedInUser)) {
            return true; // only for tests, impossible to reach during runtime
        }

        var roleName = loggedInUser.getAuthority().getName();
        var roleAuthority = loggedInUser.getAuthority().getAuthority();

        if (roleName.equals(UserRole.RESEARCHER.name())) {
            if (!documentApprovedByDefault) {
                return true;
            }
            return shouldSectionBeValidated(document, isMetadata);
        } else {
            return !roleAuthority.equals(UserRole.ADMIN.name()) &&
                !roleAuthority.equals(UserRole.INSTITUTIONAL_EDITOR.name()) &&
                !roleAuthority.equals(UserRole.INSTITUTIONAL_LIBRARIAN.name());
        }
    }

    private Boolean shouldSectionBeValidated(Document document, boolean metadata) {
        var allDocumentInstitutions = new HashSet<Integer>();

        if (document instanceof Thesis &&
            Objects.nonNull(((Thesis) document).getOrganisationUnit())) {
            allDocumentInstitutions.add(((Thesis) document).getOrganisationUnit().getId());
        } else {
            document.getContributors().stream().filter(
                c -> c.getContributionType().equals(DocumentContributionType.AUTHOR) &&
                    Objects.nonNull(c.getPerson()) && !c.getInstitutions().isEmpty()).forEach(c ->
                allDocumentInstitutions.addAll(
                    c.getInstitutions().stream().map(BaseEntity::getId).toList())
            );
        }

        return allDocumentInstitutions.stream().map(
                organisationUnitTrustConfigurationService::readTrustConfigurationForOrganisationUnit)
            .anyMatch(configuration -> metadata ? !configuration.trustNewPublications() :
                !configuration.trustNewDocumentFiles());
    }
}
