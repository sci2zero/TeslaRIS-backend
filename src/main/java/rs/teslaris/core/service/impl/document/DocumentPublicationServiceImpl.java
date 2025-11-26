package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.document.DocumentPublicationConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.DocumentIdentifierUpdateDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.document.BibliographicFormat;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ProceedingsReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Primary
@RequiredArgsConstructor
@Traceable
public class DocumentPublicationServiceImpl extends JPAServiceImpl<Document>
    implements DocumentPublicationService {

    protected final MultilingualContentService multilingualContentService;

    protected final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    protected final SearchService<DocumentPublicationIndex> searchService;

    protected final OrganisationUnitService organisationUnitService;

    protected final DocumentRepository documentRepository;

    protected final DocumentFileService documentFileService;

    protected final CitationService citationService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final PersonContributionService personContributionService;

    private final ExpressionTransformer expressionTransformer;

    private final EventService eventService;

    private final CommissionRepository commissionRepository;

    private final SearchFieldsLoader searchFieldsLoader;

    private final OrganisationUnitTrustConfigurationService
        organisationUnitTrustConfigurationService;

    private final InvolvementRepository involvementRepository;

    private final OrganisationUnitOutputConfigurationService
        organisationUnitOutputConfigurationService;

    private final Pattern doiPattern =
        Pattern.compile("^10\\.\\d{4,9}/[-,._;():a-zA-Z0-9]+$", Pattern.CASE_INSENSITIVE);

    @Value("${document.approved_by_default}")
    protected Boolean documentApprovedByDefault;

    @Value("${migration-mode.enabled}")
    private Boolean migrationModeEnabled;


    @Override
    protected JpaRepository<Document, Integer> getEntityRepository() {
        return documentRepository;
    }

    @Override
    @Transactional
    public DocumentDTO readDocumentPublication(Integer documentId) {
        return DocumentPublicationConverter.toDTO(findOne(documentId));
    }

    @Override
    @Transactional
    public String readBibliographicMetadataById(Integer documentId, BibliographicFormat format) {
        var document = findOne(documentId);

        return switch (format) {
            case BIBTEX -> StringUtil.bibTexEntryToString(
                DocumentPublicationConverter.toBibTeXEntry(document,
                    LanguageAbbreviations.ENGLISH));
            case REFMAN ->
                DocumentPublicationConverter.toTaggedFormat(document, LanguageAbbreviations.ENGLISH,
                    true);
            case ENDNOTE ->
                DocumentPublicationConverter.toTaggedFormat(document, LanguageAbbreviations.ENGLISH,
                    false);
        };
    }

    @Override
    @Transactional
    @Deprecated(forRemoval = true)
    public Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document with given id does not exist."));
    }

    @Override
    @Transactional
    @Nullable
    public Document findDocumentByOldId(Integer documentOldId) {
        var documentId = documentRepository.findDocumentByOldIdsContains(documentOldId);
        return documentId.map(this::findOne).orElse(null);
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findResearcherPublications(Integer authorId,
                                                                     List<Integer> ignore,
                                                                     List<String> tokens,
                                                                     List<DocumentPublicationType> allowedTypes,
                                                                     DocumentContributionType contributionType,
                                                                     Pageable pageable) {
        if (Objects.isNull(tokens)) {
            tokens = List.of("*");
        }

        var simpleSearchQuery =
            buildSimpleSearchQuery(tokens, null, null, null, null, allowedTypes, null);

        var contributionFilter = TermQuery.of(t -> t
            .field(getContributionField(contributionType))
            .value(authorId)
        )._toQuery();

        Query combinedQuery;

        if (Objects.nonNull(ignore) && !ignore.isEmpty()) {
            var ignoreFilter = TermsQuery.of(t -> t
                .field("databaseId")
                .terms(v -> v.value(
                    ignore.stream()
                        .map(String::valueOf)
                        .map(FieldValue::of)
                        .toList()))
            )._toQuery();

            combinedQuery = BoolQuery.of(bq -> bq
                .must(simpleSearchQuery)
                .must(contributionFilter)
                .mustNot(ignoreFilter)
            )._toQuery();
        } else {
            combinedQuery = BoolQuery.of(bq -> bq
                .must(simpleSearchQuery)
                .must(contributionFilter)
            )._toQuery();
        }

        return searchService.runQuery(combinedQuery, pageable, DocumentPublicationIndex.class,
            "document_publication");
    }

    private String getContributionField(
        DocumentContributionType contributionType) {
        return switch (contributionType) {
            case AUTHOR -> "author_ids";
            case EDITOR -> "editor_ids";
            case REVIEWER -> "reviewer_ids";
            case ADVISOR -> "advisor_ids";
            case BOARD_MEMBER -> "board_member_ids";
        };
    }

    @Override
    @Transactional
    public List<Integer> getResearchOutputIdsForDocument(Integer documentId) {
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                documentId).orElseThrow(
                () -> new NotFoundException("Document with ID " + documentId + " does not exist."))
            .getResearchOutputIds();
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findPublicationsForPublisher(Integer publisherId,
                                                                       Pageable pageable) {
        return documentPublicationIndexRepository.findByPublisherId(publisherId, pageable);
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findPublicationsForOrganisationUnit(
        Integer organisationUnitId, List<String> tokens,
        List<DocumentPublicationType> allowedTypes,
        Boolean notArchivedOnly,
        Pageable pageable) {
        var allOUIdsFromSubHierarchy =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        if (Objects.isNull(tokens)) {
            tokens = List.of("*");
        }

        var simpleSearchQuery =
            buildSimpleSearchQuery(tokens, null, null, null, null, allowedTypes, notArchivedOnly);

        var outputConfiguration =
            organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
                organisationUnitId);

        if (!outputConfiguration.showOutputs() ||
            (!outputConfiguration.showBySpecifiedAffiliation() &&
                !outputConfiguration.showByPublicationYearEmployments() &&
                !outputConfiguration.showByCurrentEmployments())) {
            return Page.empty();
        }

        List<Query> orgUnitFilters = new ArrayList<>();

        if (outputConfiguration.showBySpecifiedAffiliation()) {
            orgUnitFilters.add(TermsQuery.of(t -> t
                .field("organisation_unit_ids_specified")
                .terms(v -> v.value(
                    allOUIdsFromSubHierarchy.stream()
                        .map(String::valueOf)
                        .map(FieldValue::of)
                        .toList()))
            )._toQuery());
        }

        if (outputConfiguration.showByPublicationYearEmployments()) {
            orgUnitFilters.add(TermsQuery.of(t -> t
                .field("organisation_unit_ids_year_of_publication")
                .terms(v -> v.value(
                    allOUIdsFromSubHierarchy.stream()
                        .map(String::valueOf)
                        .map(FieldValue::of)
                        .toList()))
            )._toQuery());
        }

        if (outputConfiguration.showByCurrentEmployments()) {
            orgUnitFilters.add(TermsQuery.of(t -> t
                .field("organisation_unit_ids_active")
                .terms(v -> v.value(
                    allOUIdsFromSubHierarchy.stream()
                        .map(String::valueOf)
                        .map(FieldValue::of)
                        .toList()))
            )._toQuery());
        }

        Query institutionFilter = BoolQuery.of(bq -> bq
            .should(orgUnitFilters)
            .minimumShouldMatch("1")
        )._toQuery();

        Query combinedQuery = BoolQuery.of(bq -> bq
            .must(simpleSearchQuery)
            .must(institutionFilter)
        )._toQuery();

        return searchService.runQuery(combinedQuery, pageable, DocumentPublicationIndex.class,
            "document_publication");
    }

    @Override
    @Transactional
    public Long getPublicationCount() {
        if (SessionUtil.isUserLoggedIn()) {
            return documentPublicationIndexRepository.countPublications();
        }

        return documentPublicationIndexRepository.countApprovedPublications();
    }

    @Override
    @Transactional
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

        if (document.getIsArchived() &&
            !(migrationModeEnabled && SessionUtil.isUserLoggedInAndAdmin())) {
            throw new CantEditException("Document is archived. Can't edit.");
        }

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

        var index = findDocumentPublicationIndexByDatabaseId(documentId);
        index.setContainsFiles(
            !document.getFileItems().isEmpty() || !document.getProofs().isEmpty());
        if (!isProof) {
            indexDocumentFilesContent(document, index);
        }
        documentPublicationIndexRepository.save(index);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public void deleteDocumentFile(Integer documentId, Integer documentFileId) {
        var document = findOne(documentId);

        if (document.getIsArchived() &&
            !(migrationModeEnabled && SessionUtil.isUserLoggedInAndAdmin())) {
            throw new CantEditException("Document is archived. Can't edit.");
        }

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

        var index = findDocumentPublicationIndexByDatabaseId(documentId);
        index.setContainsFiles(
            !document.getFileItems().isEmpty() || !document.getProofs().isEmpty());
        if (!isProof) {
            indexDocumentFilesContent(document, index);
        }
        documentPublicationIndexRepository.save(index);
    }

    @Override
    @Transactional
    public void deleteDocumentPublication(Integer documentId) {
        var document = findOne(documentId);

        document.getFileItems().forEach(file -> {
            file.setDeleted(true);
            documentFileService.save(file);
        });

        document.getProofs().forEach(file -> {
            file.setDeleted(true);
            documentFileService.save(file);
        });

        if (document instanceof Thesis) {
            ((Thesis) document).getPreliminaryFiles().forEach(file -> {
                file.setDeleted(true);
                documentFileService.save(file);
            });
            ((Thesis) document).getPreliminarySupplements().forEach(file -> {
                file.setDeleted(true);
                documentFileService.save(file);
            });
            ((Thesis) document).getCommissionReports().forEach(file -> {
                file.setDeleted(true);
                documentFileService.save(file);
            });
        }

        document.getContributors().forEach(contribution -> {
            contribution.setDeleted(true);
            personContributionService.save(contribution);
        });

        documentRepository.delete(document);

        var index =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);
        index.ifPresent(documentPublicationIndexRepository::delete);

        // TODO: should we delete all document file indexes as well
    }

    @Override
    @Transactional
    public List<Integer> getContributorIds(Integer publicationId) {
        return findOne(publicationId).getContributors().stream().map(contribution -> {
            if (Objects.nonNull(contribution.getPerson())) {
                return contribution.getPerson().getId();
            }
            return -1;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void indexContributionFields(Document document, DocumentPublicationIndex index) {
        clearCommonIndexFields(index);

        setContributors(document, index);
    }

    @Override
    @Transactional(readOnly = true)
    public void indexCommonFields(Document document, DocumentPublicationIndex index) {
        var oldYear = index.getYear();
        var oldAuthors = index.getAuthorIds().stream().sorted().toList();

        clearCommonIndexFields(index);

        setBasicMetadata(document, index);
        setContributors(document, index);
        setAdditionalMetadata(document.getId(), index);

        index.setIsApproved(Objects.nonNull(document.getApproveStatus()) &&
            document.getApproveStatus().equals(ApproveStatus.APPROVED));
        index.setAreFilesValid(document.getAreFilesValid());

        if (Objects.nonNull(index.getId()) && (!index.getYear().equals(oldYear) ||
            !index.getAuthorIds().stream().sorted().toList().equals(oldAuthors))) {
            applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
                index.getAuthorIds().stream().filter(id -> id > 0).toList()));
        }
    }

    @Transactional
    private void setBasicMetadata(Document document, DocumentPublicationIndex index) {
        index.setLastEdited(Objects.nonNull(document.getLastModification())
            ? document.getLastModification()
            : new Date());
        index.setDatabaseId(document.getId());
        index.setYear(StringUtil.parseYear(document.getDocumentDate()));
        indexTitle(document, index);
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setDoi(document.getDoi());
        index.setScopusId(document.getScopusId());
        index.setOpenAlexId(document.getOpenAlexId());
        index.setWebOfScienceId(document.getWebOfScienceId());
        index.setIsOpenAccess(documentRepository.isDocumentPubliclyAvailable(document.getId()));
        indexDescription(document, index);
        indexKeywords(document, index);

        index.setContainsFiles(
            !document.getFileItems().isEmpty() || !document.getProofs().isEmpty());
        indexDocumentFilesContent(document, index);

        if (Objects.nonNull(document.getInternalIdentifiers())) {
            index.getInternalIdentifiers().clear();
            index.getInternalIdentifiers().addAll(document.getInternalIdentifiers());
        }

        if (Objects.nonNull(document.getEvent())) {
            index.setEventId(document.getEvent().getId());
        } else {
            index.setEventId(null);
        }

        index.setIsArchived(document.getIsArchived());

        index.setWordcloudTokensSr(StringUtil.extractKeywords(index.getTitleSr(),
            StringUtil.valueExists(index.getDescriptionSr()) ? index.getDescriptionSr() :
                StringUtil.sanitizeForKeywordFieldFast(index.getFullTextSr()),
            index.getKeywordsSr()));

        index.setWordcloudTokensOther(StringUtil.extractKeywords(
            MultilingualContentConverter.getLocalizedContent(document.getTitle(),
                LanguageAbbreviations.ENGLISH, LanguageAbbreviations.SERBIAN),
            (Objects.nonNull(document.getDescription()) && !document.getDescription().isEmpty()) ?
                MultilingualContentConverter.getLocalizedContent(document.getDescription(),
                    LanguageAbbreviations.ENGLISH, LanguageAbbreviations.SERBIAN) :
                StringUtil.sanitizeForKeywordFieldFast(index.getFullTextOther()),
            MultilingualContentConverter.getLocalizedContent(document.getKeywords(),
                LanguageAbbreviations.ENGLISH, LanguageAbbreviations.SERBIAN)));
    }

    private void setContributors(Document document, DocumentPublicationIndex index) {
        var organisationUnitIds = new HashSet<Integer>();
        var contributions = document.getContributors().stream()
            .sorted(Comparator.comparingInt(PersonContribution::getOrderNumber)).toList();

        var isThesis = document instanceof Thesis;
        if (isThesis && Objects.nonNull(((Thesis) document).getOrganisationUnit())) {
            organisationUnitIds.add(((Thesis) document).getOrganisationUnit().getId());
        }

        contributions.forEach(
            contribution ->
                processContribution(contribution, index, isThesis, organisationUnitIds)
        );

        index.setAuthorNamesSortable(index.getAuthorNames());

        setEmploymentIndexInformation(index, contributions, organisationUnitIds, isThesis);
    }

    private void setEmploymentIndexInformation(DocumentPublicationIndex index,
                                               List<PersonDocumentContribution> contributions,
                                               Set<Integer> specifiedContributionInstitutions,
                                               boolean isThesis) {
        var activeEmploymentInstitutions = new HashSet<Integer>();
        var yearOfPublicationEmploymentInstitutions = new HashSet<Integer>();

        contributions.forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson())) {
                involvementRepository.findEmploymentsForPerson(contribution.getPerson().getId())
                    .forEach(employment -> {
                        var orgId = employment.getOrganisationUnit().getId();

                        if (Objects.isNull(employment.getDateTo())) {
                            // Ongoing employment (dateFrom may be null or set)
                            activeEmploymentInstitutions.add(orgId);
                            var superIds =
                                organisationUnitService.getSuperOUsHierarchyRecursive(orgId);
                            activeEmploymentInstitutions.addAll(superIds);

                            if (Objects.isNull(employment.getDateFrom()) ||
                                index.getYear() >= employment.getDateFrom().getYear()) {
                                yearOfPublicationEmploymentInstitutions.add(orgId);
                                yearOfPublicationEmploymentInstitutions.addAll(superIds);
                            }
                        } else {
                            // Ended employment
                            if (Objects.nonNull(employment.getDateFrom()) &&
                                index.getYear() >= employment.getDateFrom().getYear()
                                && index.getYear() <= employment.getDateTo().getYear()) {
                                var superIds =
                                    organisationUnitService.getSuperOUsHierarchyRecursive(orgId);
                                yearOfPublicationEmploymentInstitutions.add(orgId);
                                yearOfPublicationEmploymentInstitutions.addAll(superIds);
                            }
                        }
                    });
            }
        });

        index.setOrganisationUnitIdsSpecified(new ArrayList<>(specifiedContributionInstitutions));
        index.setOrganisationUnitIdsActive(new ArrayList<>(activeEmploymentInstitutions));
        index.setOrganisationUnitIdsYearOfPublication(
            new ArrayList<>(yearOfPublicationEmploymentInstitutions));

        if (!isThesis) {
            specifiedContributionInstitutions.addAll(activeEmploymentInstitutions);
            specifiedContributionInstitutions.addAll(yearOfPublicationEmploymentInstitutions);
        }

        index.setOrganisationUnitIds(new ArrayList<>(specifiedContributionInstitutions));
    }

    private void processContribution(PersonDocumentContribution contribution,
                                     DocumentPublicationIndex index, Boolean isThesis,
                                     Set<Integer> organisationUnitIds) {
        var personExists = Objects.nonNull(contribution.getPerson());
        var contributorName =
            contribution.getAffiliationStatement().getDisplayPersonName().toString();

        if (!isThesis) {
            organisationUnitIds.addAll(contribution.getInstitutions().stream()
                .map(BaseEntity::getId)
                .toList());
        }

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

    private void setAdditionalMetadata(Integer documentId, DocumentPublicationIndex index) {
        index.setAssessedBy(
            commissionRepository.findCommissionsThatAssessedDocument(documentId));

        index.getCommissionAssessmentGroups().clear();
        index.getCommissionAssessments().clear();
        commissionRepository.findAssessmentClassificationBasicInfoForDocumentAndCommissions(
            documentId, index.getAssessedBy()).forEach(assessment -> {
            index.getCommissionAssessmentGroups().add(
                new Triple<>(assessment.commissionId(),
                    assessment.assessmentCode().substring(0, 2) + "0",
                    assessment.manual()));
            index.getCommissionAssessments().add(
                new Triple<>(assessment.commissionId(),
                    assessment.assessmentCode(),
                    assessment.manual()));
        });
    }

    @Override
    @Transactional
    public void reindexDocumentVolatileInformation(Integer documentId) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(documentIndex -> {
                setAdditionalMetadata(documentId, documentIndex);
                documentPublicationIndexRepository.save(documentIndex);
            });
    }

    @Override
    @Transactional
    public DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId) {
        var fallbackDocument = new DocumentPublicationIndex();
        fallbackDocument.setDatabaseId(documentId);
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            documentId).orElse(fallbackDocument);
    }

    @Override
    @Transactional
    public void archiveDocument(Integer documentId) {
        var document = findOne(documentId);

        if (document instanceof Thesis) {
            throw new ThesisException("Use specific thesis library endpoint to archive theses.");
        }

        if (document.getTitle().isEmpty() || Objects.isNull(document.getDocumentDate()) ||
            document.getDocumentDate().isBlank()) {
            throw new MissingDataException("missingDataToArchiveMessage");
        }

        document.setIsArchived(true);
        save(document);

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(index -> {
                index.setIsArchived(document.getIsArchived());
                documentPublicationIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void unarchiveDocument(Integer documentId) {
        var document = findOne(documentId);
        document.setIsArchived(false);

        save(document);
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(index -> {
                index.setIsArchived(document.getIsArchived());
                documentPublicationIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public synchronized void reindexEmploymentInformationForAllPersonPublications(
        Integer personId) {
        int pageNumber = 0;
        int chunkSize = 200;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var indexChunk = documentPublicationIndexRepository.findByAuthorIds(personId,
                PageRequest.of(pageNumber, chunkSize)).getContent();

            var entityChunk = documentRepository.findBulkDocuments(
                indexChunk.stream().map(DocumentPublicationIndex::getDatabaseId).toList());

            var indexMap = indexChunk.stream()
                .collect(Collectors.toMap(DocumentPublicationIndex::getDatabaseId, i -> i));

            entityChunk.forEach(document -> {
                var index = indexMap.get(document.getId());
                if (index != null) {
                    setEmploymentIndexInformation(
                        index,
                        new ArrayList<>(document.getContributors()),
                        new HashSet<>(index.getOrganisationUnitIdsSpecified()),
                        document instanceof Thesis
                    );
                    documentPublicationIndexRepository.save(index);
                }
            });

            documentPublicationIndexRepository.saveAll(indexChunk);

            pageNumber++;
            hasNextPage = indexChunk.size() == chunkSize;
        }
    }

    @Override
    @Transactional
    public void deleteNonManagedDocuments() {
        int pageSize = 100;
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            var pageResult = searchDocumentPublications(
                List.of("*"),
                PageRequest.of(page, pageSize),
                SearchRequestType.SIMPLE,
                null,
                null,
                null,
                true,
                List.of(), false
            );

            pageResult.getContent().forEach(documentIndex ->
                deleteDocumentPublication(documentIndex.getDatabaseId())
            );

            hasMore = pageResult.hasNext();
            page++;
        }
    }

    @Override
    @Transactional
    public void updateDocumentIdentifiers(Integer documentId,
                                          DocumentIdentifierUpdateDTO requestDTO) {
        var document = findOne(documentId);

        if (StringUtil.valueExists(requestDTO.getDoi())) {
            document.setDoi(requestDTO.getDoi());
        }

        if (StringUtil.valueExists(requestDTO.getScopusId())) {
            document.setScopusId(requestDTO.getScopusId());
        }

        if (StringUtil.valueExists(requestDTO.getOpenAlexId())) {
            document.setOpenAlexId(requestDTO.getOpenAlexId());
        }

        if (StringUtil.valueExists(requestDTO.getWebOfScienceId())) {
            document.setWebOfScienceId(requestDTO.getWebOfScienceId());
        }

        save(document);
    }

    @Override
    public void deleteIndexesByType(DocumentPublicationType type) {
        documentPublicationIndexRepository.deleteByType(type.name());
    }

    private void indexDocumentFilesContent(Document document, DocumentPublicationIndex index) {
        index.setFullTextSr("");
        index.setFullTextOther("");
        index.setIsOpenAccess(false);
        index.setAreFilesValid(document.getAreFilesValid());

        document.getFileItems().forEach(documentFile -> {
            if (!documentFile.getResourceType().equals(ResourceType.PREPRINT) &&
                !documentFile.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION)) {
                return;
            }

            index.setIsOpenAccess(documentRepository.isDocumentPubliclyAvailable(document.getId()));

            if (!documentFile.getMimeType().endsWith("pdf")) {
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

    protected void setCommonFields(Document document, DocumentDTO documentDTO) {
        if (document.getIsArchived()) {
            throw new CantEditException("Document is archived. Can't edit.");
        }

        document.setTitle(
            multilingualContentService.getMultilingualContent(documentDTO.getTitle()));
        document.setSubTitle(
            multilingualContentService.getMultilingualContent(documentDTO.getSubTitle()));
        document.setDescription(
            multilingualContentService.getMultilingualContent(documentDTO.getDescription()));
        document.setKeywords(
            multilingualContentService.getMultilingualContent(documentDTO.getKeywords()));
        document.setRemark(
            multilingualContentService.getMultilingualContent(documentDTO.getRemark()));

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

        if (SessionUtil.isUserLoggedIn() &&
            Objects.requireNonNull(SessionUtil.getLoggedInUser()).getAuthority().getName()
                .equals(UserRole.ADMIN.name())) {
            document.setAdminNote(documentDTO.getNote());
        }
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

        IdentifierUtil.validateAndSetIdentifier(
            documentDTO.getWebOfScienceId(),
            document.getId(),
            "\\d{15}$",
            documentRepository::existsByWebOfScienceId,
            document::setWebOfScienceId,
            "webOfScienceIdFormatError",
            "webOfScienceIdExistsError"
        );
    }

    @Override
    @Transactional
    public boolean isIdentifierInUse(String identifier, Integer documentPublicationId) {
        return documentRepository.existsByDoi(identifier, documentPublicationId) ||
            documentRepository.existsByScopusId(identifier, documentPublicationId) ||
            documentRepository.existsByOpenAlexId(identifier, documentPublicationId) ||
            documentRepository.existsByWebOfScienceId(identifier, documentPublicationId);
    }

    @Override
    @Transactional
    public boolean isDoiInUse(String doi) {
        return documentRepository.existsByDoi(doi, null);
    }

    @Override
    @Transactional
    public Pair<Long, Long> getDocumentCountsBelongingToInstitution(Integer institutionId) {
        return new Pair<>(documentPublicationIndexRepository.countAssessable(),
            documentPublicationIndexRepository.countAssessableByOrganisationUnitIds(institutionId));
    }

    @Override
    @Transactional
    public Pair<Long, Long> getAssessedDocumentCountsForCommission(Integer institutionId,
                                                                   Integer commissionId) {
        return new Pair<>(documentPublicationIndexRepository.countByAssessedBy(commissionId),
            documentPublicationIndexRepository.countByOrganisationUnitIdsAndAssessedBy(
                institutionId, commissionId));
    }

    @Override
    @Transactional
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields) {
        return searchFieldsLoader.getSearchFields("documentSearchFieldConfiguration.json",
            onlyExportFields);
    }

    @Override
    @Transactional
    public List<Pair<String, Long>> getWordCloudForSingleDocument(Integer documentId,
                                                                  DocumentPublicationType documentType,
                                                                  String language) {
        var document =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdAndType(
                    documentId, documentType.name())
                .orElseThrow(() -> new NotFoundException(
                    "Document with ID " + documentId + " does not exist."));

        var foreignLanguage = !language.startsWith("SR");
        var terms =
            foreignLanguage ? document.getWordcloudTokensOther() : document.getWordcloudTokensSr();

        Map<String, Long> result = terms.parallelStream().
            collect(Collectors.toConcurrentMap(
                w -> w, w -> 1L, Long::sum));

        return result.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(30)
            .map(entry -> new Pair<>(
                language.endsWith("-CYR") ? SerbianTransliteration.toCyrillic(entry.getKey()) :
                    entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<Document> findDocumentByCommonIdentifier(String doi, String openAlexId,
                                                             String scopusId,
                                                             String webOfScienceId) {
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

        if (Objects.isNull(webOfScienceId) || webOfScienceId.isBlank()) {
            webOfScienceId = "NOT_PRESENT";
        } else {
            webOfScienceId = webOfScienceId.replace("WOS:", "");
        }

        return documentRepository.findByOpenAlexIdOrDoiOrScopusIdOrWOSId(openAlexId, doi, scopusId,
            webOfScienceId);
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
        index.setBoardMemberNames("");

        index.getAuthorIds().clear();
        index.getEditorIds().clear();
        index.getReviewerIds().clear();
        index.getAdvisorIds().clear();
        index.getBoardMemberIds().clear();
    }

    protected void deleteProofsAndFileItems(Document publicationToDelete) {
        publicationToDelete.getProofs()
            .forEach(proof -> documentFileService.deleteDocumentFile(proof.getServerFilename()));
        publicationToDelete.getFileItems().forEach(
            fileItem -> documentFileService.deleteDocumentFile(fileItem.getServerFilename()));
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> searchDocumentPublications(List<String> tokens,
                                                                     Pageable pageable,
                                                                     SearchRequestType type,
                                                                     Integer institutionId,
                                                                     Integer commissionId,
                                                                     Boolean authorReprint,
                                                                     Boolean unmanaged,
                                                                     List<DocumentPublicationType> allowedTypes,
                                                                     Boolean notArchivedOnly) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(
                buildSimpleSearchQuery(tokens, institutionId, commissionId, authorReprint,
                    unmanaged, allowedTypes, notArchivedOnly),
                pageable,
                DocumentPublicationIndex.class, "document_publication");
        }

        return searchService.runQuery(
            buildAdvancedSearchQuery(tokens, institutionId, commissionId, authorReprint,
                unmanaged, allowedTypes, notArchivedOnly), pageable,
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findDocumentDuplicates(List<String> titles,
                                                                 String doi,
                                                                 String scopusId,
                                                                 String openAlexId,
                                                                 String webOfScienceId,
                                                                 List<String> internalIdentifiers) {
        var query =
            buildDeduplicationSearchQuery(titles, doi, scopusId, openAlexId, webOfScienceId,
                internalIdentifiers);
        return searchService.runQuery(query,
            Pageable.ofSize(5),
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findNonAffiliatedDocuments(Integer organisationUnitId,
                                                                     Integer personId,
                                                                     Pageable pageable) {
        var nonAffiliatedDocumentIds =
            personContributionService.getIdsOfNonRelatedDocuments(organisationUnitId, personId);
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(
            nonAffiliatedDocumentIds, pageable);
    }

    @Override
    @Transactional
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
    @Transactional
    public void deleteIndexes() {
        documentPublicationIndexRepository.deleteAll();
    }

    @Override
    @Transactional
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
    @Transactional
    public void unbindResearcherFromContribution(Integer personId, Integer documentId) {
        var contribution =
            personContributionService.findContributionForResearcherAndDocument(personId,
                documentId);

        if (Objects.isNull(contribution)) {
            return;
        }

        var unbindedAuthorActiveEmployments =
            involvementRepository.findActiveEmploymentInstitutionIds(
                contribution.getPerson().getId());
        migrateContributionToUnmanaged(contribution, true, Collections.emptySet());

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

            var shouldNotifyInstitutionalEditors = new AtomicBoolean(false);
            document.get().getContributors().forEach(otherContribution -> {
                if (Objects.isNull(otherContribution.getPerson())) {
                    return;
                }

                var authorEmployments = involvementRepository.findActiveEmploymentInstitutionIds(
                    otherContribution.getPerson().getId());
                if (!Collections.disjoint(unbindedAuthorActiveEmployments, authorEmployments)) {
                    shouldNotifyInstitutionalEditors.set(true);
                }

                personContributionService.getUserForContributor(
                        otherContribution.getPerson().getId())
                    .ifPresent(user -> {
                        var notificationValues = new HashMap<String, String>();
                        notificationValues.put("title", document.get().getTitle().stream()
                            .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get()
                            .getContent());
                        notificationValues.put("author",
                            contribution.getAffiliationStatement().getDisplayPersonName().toText());
                        notificationValues.put("contributionId",
                            otherContribution.getId().toString());
                        notificationValues.put("documentId", document.get().getId().toString());
                        notificationValues.put("personId",
                            otherContribution.getPerson().getId().toString());
                        personContributionService.notifyContributor(
                            NotificationFactory.constructAuthorUnbindedFromPublicationNotification(
                                notificationValues, user, false),
                            NotificationType.NEW_AUTHOR_UNBINDING);
                    });
            });

            var notifyAdmin = index.getAuthorIds().stream().noneMatch(id -> id > 0);
            if (notifyAdmin) {
                personContributionService.notifyAdminsAboutUnbindedContribution(document.get());
            } else if (shouldNotifyInstitutionalEditors.get()) {
                notifyInstitutionalEditors(contribution, unbindedAuthorActiveEmployments,
                    document.get());
            }
        }
    }

    private void notifyInstitutionalEditors(PersonDocumentContribution contribution,
                                            List<Integer> activeEmployments, Document document) {
        var notifiableInstitutionIds = new HashSet<Integer>();
        activeEmployments.forEach(employmentInstitutionId -> {
            notifiableInstitutionIds.add(employmentInstitutionId);
            notifiableInstitutionIds.addAll(
                organisationUnitService.getSuperOUsHierarchyRecursive(employmentInstitutionId));
        });

        personContributionService.getEditorUsersForContributionInstitutionIds(
            notifiableInstitutionIds).forEach(institutionalEditorUser -> {
            var notificationValues = new HashMap<String, String>();
            notificationValues.put("title", document.getTitle().stream()
                .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get()
                .getContent());
            notificationValues.put("author",
                contribution.getAffiliationStatement().getDisplayPersonName().toText());
            notificationValues.put("documentId", document.getId().toString());
            notificationValues.put("institutionId",
                institutionalEditorUser.getOrganisationUnit().getId().toString());

            personContributionService.notifyContributor(
                NotificationFactory.constructAuthorUnbindedFromPublicationNotification(
                    notificationValues, institutionalEditorUser, true),
                NotificationType.NEW_EMPLOYED_RESEARCHER_UNBINDED);
        });
    }

    @Override
    @Transactional
    public void unbindInstitutionResearchersFromDocument(Integer institutionId,
                                                         Integer documentId) {
        var allPossibleInstitutions =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

        documentRepository.findById(documentId).ifPresent(document -> {
            if (document instanceof Thesis || Objects.isNull(document.getContributors())) {
                return;
            }

            document.getContributors().forEach(contribution -> {
                if (Objects.isNull(contribution.getPerson())) {
                    return;
                }

                var user = contribution.getPerson().getUser();

                var employmentIds = involvementRepository.findActiveEmploymentInstitutionIds(
                    contribution.getPerson().getId());
                var specifiedEmploymentIds =
                    contribution.getInstitutions().stream().map(OrganisationUnit::getId)
                        .collect(Collectors.toSet());

                var employmentIntersection = allPossibleInstitutions.stream()
                    .filter(employmentIds::contains)
                    .collect(Collectors.toSet());

                var specifiedInstitutionsIntersection = allPossibleInstitutions.stream()
                    .filter(specifiedEmploymentIds::contains)
                    .collect(Collectors.toSet());

                if (!employmentIntersection.isEmpty() ||
                    !specifiedInstitutionsIntersection.isEmpty()) {
                    migrateContributionToUnmanaged(contribution, !employmentIntersection.isEmpty(),
                        specifiedInstitutionsIntersection);

                    if (Objects.nonNull(user)) {
                        notifyUnbindedAuthorUser(document, contribution, user);
                    }

                }
            });

            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
                .ifPresent(index -> {
                    indexCommonFields(document, index);
                    documentPublicationIndexRepository.save(index);

                    var notifyAdmin = !index.getAuthorIds().isEmpty() &&
                        index.getAuthorIds().stream().noneMatch(id -> id > 0);
                    if (notifyAdmin) {
                        personContributionService.notifyAdminsAboutUnbindedContribution(document);
                    }
                });
        });
    }

    private void migrateContributionToUnmanaged(PersonDocumentContribution contribution,
                                                Boolean deletePerson,
                                                Set<Integer> specifiedInstitutionsIntersection) {
        if (deletePerson) {
            contribution.setPerson(null);
            contribution.getInstitutions().clear();
        } else {
            specifiedInstitutionsIntersection.forEach(institutionId -> contribution.setInstitutions(
                contribution.getInstitutions().stream()
                    .filter(i -> !Objects.equals(i.getId(), institutionId))
                    .collect(Collectors.toSet())));
        }

        contribution.setIsCorrespondingContributor(false);
        contribution.setEmploymentTitle(null);

        if (Objects.nonNull(contribution.getAffiliationStatement()) &&
            Objects.nonNull(contribution.getAffiliationStatement().getContact())) {
            contribution.getAffiliationStatement().getContact().setContactEmail("");
            contribution.getAffiliationStatement().getContact().setPhoneNumber("");
        }

        personContributionService.save(contribution);
    }

    private void notifyUnbindedAuthorUser(Document document,
                                          PersonDocumentContribution contribution, User user) {
        var notificationValues = new HashMap<String, String>();
        notificationValues.put("title", document.getTitle().stream()
            .max(Comparator.comparingInt(MultiLingualContent::getPriority)).get()
            .getContent());
        notificationValues.put("personId", user.getPerson().getId().toString());
        notificationValues.put("documentId", document.getId().toString());
        notificationValues.put("contributionId", contribution.getId().toString());

        personContributionService.notifyContributor(
            NotificationFactory.constructAuthorUnbindedByEditorNotification(notificationValues,
                user), NotificationType.AUTHOR_UNBINDED_BY_EDITOR);
    }

    private Query buildDeduplicationSearchQuery(List<String> titles, String doi, String scopusId,
                                                String openAlexId, String webOfScienceId,
                                                List<String> internalIdentifiers) {
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
                            m -> m.field("scopus_id").query(scopusId)));
                    }

                    if (Objects.nonNull(doi) && !doi.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("doi").query(doi)));
                    }

                    if (Objects.nonNull(openAlexId) && !openAlexId.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("open_alex_id").query(openAlexId)));
                    }

                    if (Objects.nonNull(webOfScienceId) && !webOfScienceId.isBlank()) {
                        eq.should(sb -> sb.match(
                            m -> m.field("web_of_science_id").query(webOfScienceId)));
                    }

                    if (Objects.nonNull(internalIdentifiers) && !internalIdentifiers.isEmpty()) {
                        var internalIdentifierTerms = new TermsQueryField.Builder()
                            .value(internalIdentifiers.stream().map(FieldValue::of).toList())
                            .build();
                        eq.should(sb -> sb.terms(
                            t -> t.field("internal_identifiers").terms(internalIdentifierTerms)
                        ));
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
                                           Boolean authorReprint,
                                           Boolean unmanaged,
                                           List<DocumentPublicationType> allowedTypes,
                                           Boolean notArchivedOnly) {
        return BoolQuery.of(b -> {
            if (Objects.nonNull(institutionId) && institutionId > 0) {
                var allSubInstitutions =
                    organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

                b.must(q -> q.terms(t -> t.field("organisation_unit_ids").terms(
                    terms -> terms.value(
                        allSubInstitutions.stream().map(id -> FieldValue.of(id.toString()))
                            .collect(Collectors.toList())))));
            }

            if (Objects.nonNull(commissionId) && commissionId > 0) {
                b.mustNot(q -> q.term(t -> t.field("assessed_by").value(commissionId)));
            }

            if (Objects.nonNull(authorReprint) && authorReprint) {
                b.must(q -> q.term(t -> t.field("author_reprint").value(true)));
            }

            if (Objects.nonNull(notArchivedOnly) && notArchivedOnly) {
                b.must(q -> q.term(t -> t.field("is_archived").value(false)));
            }

            if (Objects.nonNull(allowedTypes) && !allowedTypes.isEmpty()) {
                b.must(createTypeTermsQuery(allowedTypes));
            }

            if (!SessionUtil.isUserLoggedIn()) {
                b.must(q -> q.term(t -> t.field("is_approved").value(true)));
            }

            if (Objects.nonNull(unmanaged) && unmanaged) {
                var allAuthorsUnmanagedScriptQuery =
                    "if (!doc.containsKey('author_ids') || doc['author_ids'].size() == 0) return false; "
                        + "for (id in doc['author_ids']) { if (id != -1) return false; } "
                        + "return true;";

                b.must(sb -> sb.script(s -> s
                    .script(scr -> scr
                        .inline(i -> i.source(allAuthorsUnmanagedScriptQuery))
                    )
                ));
            }

            return b;
        })._toQuery();
    }

    private Query buildSimpleTokenQuery(List<String> tokens, String minShouldMatch) {
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
                } else if (doiPattern.matcher(StringUtil.performDOIPreprocessing(token))
                    .matches()) {
                    String normalizedToken = StringUtil.performDOIPreprocessing(token);

                    eq.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("doi").value(normalizedToken)
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("doi").value(normalizedToken)
                                .caseInsensitive(true)))
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
                    .should(sb -> sb.match(m -> m.field("editor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("reviewer_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("advisor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("type").query(token)))
                    .should(sb -> sb.match(m -> m.field("doi").query(token)));

                // TODO: Should we be this restrictive?
                if (SessionUtil.isUserLoggedIn()) {
                    eq.should(sb -> sb.match(m -> m.field("full_text_sr").query(token).boost(0.7f)))
                        .should(sb -> sb.match(
                            m -> m.field("full_text_other").query(token).boost(0.7f)));
                }
            });

            return eq.minimumShouldMatch(minShouldMatch);
        })._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         Integer institutionId,
                                         Integer commissionId,
                                         Boolean authorReprint,
                                         Boolean unmanaged,
                                         List<DocumentPublicationType> allowedTypes,
                                         Boolean notArchivedOnly) {
        String minShouldMatch;
        if (tokens.size() <= 2) {
            minShouldMatch = "1"; // Allow partial match for very short queries
        } else {
            minShouldMatch = String.valueOf(Math.min((int) Math.ceil(tokens.size() * 0.7), 10));
        }

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildSimpleMetadataQuery(institutionId, commissionId, authorReprint, unmanaged,
                allowedTypes, notArchivedOnly));
            b.must(buildSimpleTokenQuery(tokens, minShouldMatch));
            b.mustNot(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.PROCEEDINGS.name())));
            return b;
        })))._toQuery();
    }

    public Query buildAdvancedSearchQuery(List<String> tokens,
                                          Integer institutionId,
                                          Integer commissionId,
                                          Boolean authorReprint,
                                          Boolean unmanaged,
                                          List<DocumentPublicationType> allowedTypes,
                                          Boolean notArchivedOnly) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildSimpleMetadataQuery(institutionId, commissionId, authorReprint, unmanaged,
                allowedTypes, notArchivedOnly));
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
                    NotificationFactory.contructAddedToPublicationNotification(notificationValues,
                        userOptional.get()), NotificationType.ADDED_TO_PUBLICATION);
            }
        });
    }

    protected void clearIndexWhenFailedRead(Integer documentId, DocumentPublicationType type) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdAndType(
            documentId, type.name()).ifPresent(documentPublicationIndexRepository::delete);
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
        var loggedInUser = SessionUtil.getLoggedInUser();

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
                !roleAuthority.equals(UserRole.INSTITUTIONAL_LIBRARIAN.name()) &&
                !roleAuthority.equals(UserRole.HEAD_OF_LIBRARY.name());
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

    @Async
    @EventListener
    protected void handlePersonEmploymentOUHierarchyStructureChangedEvent(
        PersonEmploymentOUHierarchyStructureChangedEvent event) {
        reindexEmploymentInformationForAllPersonPublications(event.getPersonId());
    }
}
