package rs.teslaris.core.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.DocumentPublicationService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class DocumentPublicationServiceImpl implements DocumentPublicationService {

    protected final DocumentRepository documentRepository;

    protected final DocumentFileService documentFileService;

    protected final MultilingualContentService multilingualContentService;

    private final PersonContributionService personContributionService;

    @Value("${document.approved_by_default}")
    protected Boolean documentApprovedByDefault;


    @Override
    public Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document with given id does not exist."));
    }

    @Override
    public void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved) {
        var documentToUpdate = findDocumentById(documentId);

        if (documentToUpdate.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            documentToUpdate.setApproveStatus(
                isApproved ? ApproveStatus.APPROVED : ApproveStatus.DECLINED);
        }

        documentRepository.save(documentToUpdate);
    }

    @Override
    public void addDocumentFile(Integer documentId, List<DocumentFileDTO> documentFiles,
                                Boolean isProof) {
        var document = findDocumentById(documentId);
        documentFiles.forEach(file -> {
            var documentFile = documentFileService.saveNewDocument(file);
            if (isProof) {
                document.getProofs().add(documentFile);
            } else {
                document.getFileItems().add(documentFile);
            }
            documentRepository.save(document);
        });
    }

    @Override
    public void deleteDocumentFile(Integer documentId, Integer documentFileId, Boolean isProof) {
        var document = findDocumentById(documentId);
        var documentFile = documentFileService.findDocumentFileById(documentFileId);

        if (isProof) {
            document.getProofs().remove(documentFile);
        } else {
            document.getFileItems().remove(documentFile);
        }
        documentRepository.save(document);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    public List<Integer> getContributorIds(Integer publicationId) {
        return findDocumentById(publicationId).getContributors().stream().map(BaseEntity::getId)
            .filter(Objects::nonNull).collect(Collectors.toList());
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
    }

    protected void clearCommonFields(Document publication) {
        publication.getTitle().clear();
        publication.getSubTitle().clear();
        publication.getDescription().clear();
        publication.getKeywords().clear();
        publication.getContributors().clear();
    }
}
