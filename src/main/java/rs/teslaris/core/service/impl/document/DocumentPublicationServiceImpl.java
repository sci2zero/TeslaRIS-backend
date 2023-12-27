package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
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
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;

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
        // TODO: Check if i can change to findOne
        var documentFile = documentFileService.findDocumentFileById(documentFileId);

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

        // TODO: Check if calling this method is neccesseary
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());

        if (document.getApproveStatus().equals(ApproveStatus.APPROVED) && !isProof) {
            indexDocumentFilesContent(document,
                findDocumentPublicationIndexByDatabaseId(documentId));
        }
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
        document.getTitle().forEach(mc -> {
            if (mc.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN)) {
                index.setTitleSr(mc.getContent());
            } else {
                index.setTitleOther(mc.getContent());
            }
        });
        document.getSubTitle().forEach(mc -> {
            if (mc.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN)) {
                index.setTitleSr(index.getTitleSr() + " " + mc.getContent());
            } else {
                index.setTitleOther(index.getTitleOther() + " " + mc.getContent());
            }
        });
    }

    private void indexDescription(Document document, DocumentPublicationIndex index) {
        document.getDescription().forEach(mc -> {
            if (mc.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN)) {
                index.setDescriptionSr(mc.getContent());
            } else {
                index.setDescriptionOther(mc.getContent());
            }
        });
    }

    private void indexKeywords(Document document, DocumentPublicationIndex index) {
        document.getKeywords().forEach(mc -> {
            if (mc.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN)) {
                index.setKeywordsSr(mc.getContent());
            } else {
                index.setKeywordsOther(mc.getContent());
            }
        });
    }

    private int parseYear(String dateString) {
        DateTimeFormatter[] formatters =
            {DateTimeFormatter.ofPattern("yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy.")};

        for (var formatter : formatters) {
            try {
                var date = LocalDate.parse(dateString, formatter);
                return date.getYear();
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

        document.setEvent(eventService.findEventById(documentDTO.getEventId()));
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
            tokens.forEach(token -> {
                b.should(sb -> sb.match(
                    m -> m.field("title_sr").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("title_other").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("description_sr").fuzziness(Fuzziness.ONE.asString())
                        .query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("description_other").fuzziness(Fuzziness.ONE.asString())
                        .query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("keywords_sr").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("keywords_other").fuzziness(Fuzziness.ONE.asString())
                        .query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("full_text_sr").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("full_text_other").fuzziness(Fuzziness.ONE.asString())
                        .query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("authorNames").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("editorNames").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("reviewerNames").fuzziness(Fuzziness.ONE.asString())
                        .query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("advisorNames").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("type").fuzziness(Fuzziness.ONE.asString()).query(token)));
            });
            return b;
        })))._toQuery();
    }
}
