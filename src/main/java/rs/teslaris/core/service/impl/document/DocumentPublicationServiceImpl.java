package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import rs.teslaris.core.converter.document.DocumentFileConverter;
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
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ProceedingsReferenceConstraintViolationException;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class DocumentPublicationServiceImpl extends JPAServiceImpl<Document>
    implements DocumentPublicationService {

    protected final MultilingualContentService multilingualContentService;

    protected final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    protected final SearchService<DocumentPublicationIndex> searchService;

    protected final OrganisationUnitService organisationUnitService;

    private final DocumentRepository documentRepository;

    private final DocumentFileService documentFileService;

    private final PersonContributionService personContributionService;

    private final ExpressionTransformer expressionTransformer;

    private final EventService eventService;

    @Value("${document.approved_by_default}")
    protected Boolean documentApprovedByDefault;


    @Override
    protected JpaRepository<Document, Integer> getEntityRepository() {
        return documentRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document with given id does not exist."));
    }

    @Override
    @Nullable
    public Document findDocumentByOldId(Integer documentId) {
        return documentRepository.findDocumentByOldId(documentId).orElse(null);
    }

    @Override
    public Page<DocumentPublicationIndex> findResearcherPublications(Integer authorId,
                                                                     Pageable pageable) {
        return documentPublicationIndexRepository.findByAuthorIds(authorId, pageable);
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForPublisher(Integer publisherId,
                                                                       Pageable pageable) {
        return documentPublicationIndexRepository.findByPublisherId(publisherId, pageable);
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForOrganisationUnit(
        Integer organisationUnitId, Pageable pageable) {
        var allOUIdsFromSubHierarchy =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        return documentPublicationIndexRepository.findByOrganisationUnitIdsIn(
            allOUIdsFromSubHierarchy, pageable);
    }

    @Override
    public Long getPublicationCount() {
        return documentPublicationIndexRepository.count();
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
        var documentFile = documentFileService.saveNewDocument(file, !isProof);
        if (isProof) {
            document.getProofs().add(documentFile);
        } else {
            document.getFileItems().add(documentFile);
        }
        documentRepository.save(document);

        if (!isProof && document.getApproveStatus().equals(ApproveStatus.APPROVED)) {
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

        if (document.getApproveStatus().equals(ApproveStatus.APPROVED) && !isProof) {
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

        index.setDatabaseId(document.getId());
        index.setYear(parseYear(document.getDocumentDate()));
        indexTitle(document, index);
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setDoi(document.getDoi());
        index.setScopusId(document.getScopusId());
        indexDescription(document, index);
        indexKeywords(document, index);
        indexDocumentFilesContent(document, index);

        if (Objects.nonNull(document.getEvent())) {
            index.setEventId(document.getEvent().getId());
        }

        var organisationUnitIds = new ArrayList<Integer>();

        document.getContributors()
            .stream().sorted(Comparator.comparingInt(PersonContribution::getOrderNumber))
            .forEach(contribution -> {
                var personExists = Objects.nonNull(contribution.getPerson());

                var contributorName =
                    contribution.getAffiliationStatement().getDisplayPersonName().toString();

                organisationUnitIds.addAll(
                    contribution.getInstitutions().stream().map((BaseEntity::getId)).toList());

                switch (contribution.getContributionType()) {
                    case AUTHOR:
                        if (contribution.getIsCorrespondingContributor()) {
                            contributorName += "*";
                        }

                        if (personExists) {
                            index.getAuthorIds().add(contribution.getPerson().getId());
                        } else {
                            index.getAuthorIds().add(-1);
                        }

                        index.setAuthorNames(StringUtil.removeLeadingColonSpace(
                            index.getAuthorNames() + "; " + contributorName));
                        break;
                    case EDITOR:
                        if (personExists) {
                            index.getEditorIds().add(contribution.getPerson().getId());
                        } else {
                            index.getEditorIds().add(-1);
                        }

                        index.setEditorNames(StringUtil.removeLeadingColonSpace(
                            index.getEditorNames() + "; " + contributorName));
                        break;
                    case ADVISOR:
                        if (personExists) {
                            index.getAdvisorIds().add(contribution.getPerson().getId());
                        } else {
                            index.getAdvisorIds().add(-1);
                        }

                        index.setAdvisorNames(StringUtil.removeLeadingColonSpace(
                            index.getAdvisorNames() + "; " + contributorName));
                        break;
                    case REVIEWER:
                        if (personExists) {
                            index.getReviewerIds().add(contribution.getPerson().getId());
                        } else {
                            index.getReviewerIds().add(-1);
                        }

                        index.setReviewerNames(StringUtil.removeLeadingColonSpace(
                            index.getReviewerNames() + "; " + contributorName));
                        break;
                    case BOARD_MEMBER:
                        if (personExists) {
                            index.getBoardMemberIds().add(contribution.getPerson().getId());
                        } else {
                            index.getBoardMemberIds().add(-1);
                        }

                        index.setBoardMemberNames(StringUtil.removeLeadingColonSpace(
                            index.getBoardMemberNames() + "; " + contributorName));
                        break;
                }
            });
        index.setAuthorNamesSortable(index.getAuthorNames());
        index.setOrganisationUnitIds(organisationUnitIds);
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
        document.getFileItems().forEach(documentFile -> {
            if (!documentFile.getMimeType().contains("pdf")) {
                return;
            }
            var file = documentFileService.findDocumentFileIndexByDatabaseId(documentFile.getId());
            index.setFullTextSr(index.getFullTextSr() + file.getPdfTextSr());
            index.setFullTextOther(index.getFullTextOther() + " " + file.getPdfTextOther());
        });
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

        document.setOldId(documentDTO.getOldId());
        document.setDocumentDate(documentDTO.getDocumentDate());

        setUris(document, documentDTO);
        setCommonIdentifiers(document, documentDTO);

        document.setScopusId(documentDTO.getScopusId());

        if (Objects.nonNull(documentDTO.getEventId())) {
            var event = eventService.findEventById(documentDTO.getEventId());

            if (event.getSerialEvent()) {
                throw new ProceedingsReferenceConstraintViolationException(
                    "Proceedings cannot be bound to serial event.");
            }

            document.setEvent(event);
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
            "^\\d{6,11}$",
            documentRepository::existsByScopusId,
            document::setScopusId,
            "scopusIdFormatError",
            "scopusIdExistsError"
        );
    }

    private void setUris(Document document, DocumentDTO documentDTO) {
        var uriPattern =
            "^(?:(?:http|https)://)(?:\\S+(?::\\S*)?@)?(?:(?:(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[0-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))|localhost)(?::\\d{2,5})?(?:(/|\\?|#)[^\\s]*)?$";
        var pattern = Pattern.compile(uriPattern, Pattern.CASE_INSENSITIVE);

        if (Objects.nonNull(documentDTO.getUris())) {
            documentDTO.getUris().forEach(uri -> {
                if (uri.length() <= 2048 && pattern.matcher(uri).matches()) {
                    document.getUris().add(uri);
                }
            });
        }
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
                                                                     SearchRequestType type) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(buildSimpleSearchQuery(tokens),
                pageable,
                DocumentPublicationIndex.class, "document_publication");
        }

        return searchService.runQuery(
            expressionTransformer.parseAdvancedQuery(tokens), pageable,
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    public Page<DocumentPublicationIndex> findDocumentDuplicates(List<String> titles, String doi,
                                                                 String scopusId) {
        var query = buildDeduplicationSearchQuery(titles, doi, scopusId);
        return searchService.runQuery(query,
            Pageable.ofSize(5),
            DocumentPublicationIndex.class, "document_publication");
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
            var index = indexOptional.get();
            indexCommonFields(document.get(), index);
            documentPublicationIndexRepository.save(index);
        }
    }

    private Query buildDeduplicationSearchQuery(List<String> titles, String doi, String scopusId) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    titles.forEach(title -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_sr").query(title)));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_other").query(title)));
                    });
                    eq.should(sb -> sb.match(
                        m -> m.field("scopusId").query(scopusId)));
                    eq.should(sb -> sb.match(
                        m -> m.field("doi").query(doi)));
                    return eq;
                });
                return bq;
            });
            b.must(bq -> {
                bq.bool(eq -> {
                    eq.should(sb -> sb.match(
                        m -> m.field("type")
                            .query(DocumentPublicationType.JOURNAL_PUBLICATION.name())));
                    eq.should(sb -> sb.match(
                        m -> m.field("type")
                            .query(DocumentPublicationType.PROCEEDINGS_PUBLICATION.name())));
                    return eq;
                });
                return bq;
            });
            return b;
        })))._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                            b.must(mp ->
                                mp.bool(m -> {
                                    {
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("title_sr")
                                                .query(token.replace("\\\"", ""))));
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("title_other")
                                                .query(token.replace("\\\"", ""))));
                                    }
                                    return m;
                                }));
                        }

                        eq.should(sb -> sb.wildcard(
                            m -> m.field("title_sr").value(token + "*").caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("title_sr").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("title_other").value(token + "*").caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_other").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("keywords_sr").value("*" + token + "*")));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("keywords_other").value("*" + token + "*")));
                        eq.should(sb -> sb.match(
                            m -> m.field("full_text_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("full_text_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("author_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("editor_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("reviewer_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("advisor_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("type").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("doi").query(token)));
                    });
                    return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                });
                return bq;
            });
            b.mustNot(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.PROCEEDINGS.name())));
            return b;
        })))._toQuery();
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
}
