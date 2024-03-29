package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
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

    private final DocumentRepository documentRepository;

    private final DocumentFileService documentFileService;

    private final PersonContributionService personContributionService;

    private final SearchService<DocumentPublicationIndex> searchService;

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
    public void addDocumentFile(Integer documentId, List<DocumentFileDTO> documentFiles,
                                Boolean isProof) {
        var document = findOne(documentId);
        documentFiles.forEach(file -> {
            var documentFile = documentFileService.saveNewDocument(file, !isProof);
            if (isProof) {
                document.getProofs().add(documentFile);
            } else {
                document.getFileItems().add(documentFile);
            }
            documentRepository.save(document);
        });

        if (document.getApproveStatus().equals(ApproveStatus.APPROVED) && !isProof) {
            indexDocumentFilesContent(document,
                findDocumentPublicationIndexByDatabaseId(documentId));
        }
    }

    @Override
    @Transactional
    public void deleteDocumentFile(Integer documentId, Integer documentFileId, Boolean isProof) {
        var document = findOne(documentId);
        var documentFile = documentFileService.findOne(documentFileId);

        if (isProof) {
            Set<DocumentFile> proofs = document.getProofs();
            proofs.forEach(p -> p.setDeleted(true));
            documentFileService.saveAll(proofs);
        } else {
            Set<DocumentFile> fileItems = document.getFileItems();
            fileItems.forEach(p -> p.setDeleted(true));
            documentFileService.saveAll(fileItems);
        }
        documentRepository.save(document);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());

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
        return findOne(publicationId).getContributors().stream().map(BaseEntity::getId)
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
        indexDescription(document, index);
        indexKeywords(document, index);
        indexDocumentFilesContent(document, index);

        document.getContributors().forEach(contribution -> {
            var personExists = contribution.getPerson() != null;

            var contributorDisplayName =
                contribution.getAffiliationStatement().getDisplayPersonName();
            var contributorName =
                Objects.toString(contributorDisplayName.getFirstname(), "") + " " +
                    Objects.toString(contributorDisplayName.getLastname(), "");

            switch (contribution.getContributionType()) {
                case AUTHOR:
                    if (personExists) {
                        index.getAuthorIds().add(contribution.getPerson().getId());
                    }
                    index.setAuthorNames(index.getAuthorNames() + ", " + contributorName);
                    break;
                case EDITOR:
                    if (personExists) {
                        index.getEditorIds().add(contribution.getPerson().getId());
                    }
                    index.setEditorNames(index.getEditorNames() + ", " + contributorName);
                    break;
                case ADVISOR:
                    if (personExists) {
                        index.getAdvisorIds().add(contribution.getPerson().getId());
                    }
                    index.setAdvisorNames(index.getAdvisorNames() + ", " + contributorName);
                    break;
                case REVIEWER:
                    if (personExists) {
                        index.getReviewerIds().add(contribution.getPerson().getId());
                    }
                    index.setReviewerNames(index.getReviewerNames() + ", " + contributorName);
                    break;
            }
        });
    }

    @Override
    public DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId) {
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            documentId).orElseThrow(() -> new NotFoundException(
            "Document publication index with given ID does not exist."));
    }

    private void indexDocumentFilesContent(Document document, DocumentPublicationIndex index) {
        index.setFullTextSr("");
        index.setFullTextOther("");
        document.getFileItems().forEach(documentFile -> {
            var file = documentFileService.findDocumentFileIndexByDatabaseId(documentFile.getId());
            index.setFullTextSr(index.getFullTextSr() + file.getPdfTextSr());
            index.setFullTextOther(index.getFullTextOther() + " " + file.getPdfTextOther());
        });
    }

    private void indexTitle(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getTitle());
        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getSubTitle());

        StringUtil.removeTrailingPipeDelimiter(contentSr, contentOther);
        index.setTitleSr(contentSr.length() > 0 ? contentSr.toString() : contentOther.toString());
        index.setTitleOther(
            contentOther.length() > 0 ? contentOther.toString() : contentSr.toString());
    }

    private void indexDescription(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getDescription());

        StringUtil.removeTrailingPipeDelimiter(contentSr, contentOther);
        index.setDescriptionSr(
            contentSr.length() > 0 ? contentSr.toString() : contentOther.toString());
        index.setDescriptionOther(
            contentOther.length() > 0 ? contentOther.toString() : contentSr.toString());
    }

    private void indexKeywords(Document document, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            document.getKeywords());

        StringUtil.removeTrailingPipeDelimiter(contentSr, contentOther);
        index.setKeywordsSr(
            contentSr.length() > 0 ? contentSr.toString() : contentOther.toString());
        index.setKeywordsOther(
            contentOther.length() > 0 ? contentOther.toString() : contentSr.toString());
    }

    private int parseYear(String dateString) {
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy"), // Year only
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

        document.setUris(documentDTO.getUris());
        document.setDocumentDate(documentDTO.getDocumentDate());
        document.setDoi(documentDTO.getDoi());
        document.setScopusId(documentDTO.getScopusId());

        document.setProofs(new HashSet<>());
        document.setFileItems(new HashSet<>());

        if (Objects.nonNull(documentDTO.getEventId())) {
            document.setEvent(eventService.findEventById(documentDTO.getEventId()));
        }
    }

    protected void clearCommonFields(Document publication) {
        publication.getTitle().clear();
        publication.getSubTitle().clear();
        publication.getDescription().clear();
        publication.getKeywords().clear();
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

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        b.should(sb -> sb.wildcard(
                            m -> m.field("title_sr").value(token).caseInsensitive(true)));
                        b.should(sb -> sb.match(
                            m -> m.field("title_sr").query(token)));
                        b.should(sb -> sb.wildcard(
                            m -> m.field("title_other").value(token).caseInsensitive(true)));
                        b.should(sb -> sb.match(
                            m -> m.field("description_sr").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("description_other").query(token)));
                        b.should(sb -> sb.wildcard(
                            m -> m.field("keywords_sr").value("*" + token + "*")));
                        b.should(sb -> sb.wildcard(
                            m -> m.field("keywords_other").value("*" + token + "*")));
                        b.should(sb -> sb.match(
                            m -> m.field("full_text_sr").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("full_text_other").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("authorNames").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("editorNames").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("reviewerNames").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("advisorNames").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("type").query(token)));
                        b.should(sb -> sb.match(
                            m -> m.field("doi").query(token)));
                    });
                    return eq;
                });
                return bq;
            });
            b.mustNot(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.PROCEEDINGS.name())));
            return b;
        })))._toQuery();
    }
}
