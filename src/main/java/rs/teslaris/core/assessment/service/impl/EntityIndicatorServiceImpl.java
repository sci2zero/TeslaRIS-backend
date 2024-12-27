package rs.teslaris.core.assessment.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.dto.EntityIndicatorDTO;
import rs.teslaris.core.assessment.model.EntityIndicator;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.EntityIndicatorService;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditEntityIndicatorException;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EntityIndicatorServiceImpl extends JPAServiceImpl<EntityIndicator> implements
    EntityIndicatorService {

    private final EntityIndicatorRepository entityIndicatorRepository;

    private final DocumentFileService documentFileService;

    private final IndicatorService indicatorService;


    @Override
    public EntityIndicator findByUserId(Integer userId) {
        return entityIndicatorRepository.findByUserId(userId);
    }

    @Override
    public boolean isUserTheOwnerOfEntityIndicator(Integer userId, Integer entityIndicatorId) {
        return entityIndicatorRepository.isUserTheOwnerOfEntityIndicator(userId, entityIndicatorId);
    }

    @Override
    public DocumentFileResponseDTO addEntityIndicatorProof(DocumentFileDTO proof,
                                                           Integer entityIndicatorId) {
        var entityIndicator = findOne(entityIndicatorId);
        var documentFile = documentFileService.saveNewDocument(proof, false);
        entityIndicator.getProofs().add(documentFile);

        save(entityIndicator);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public DocumentFileResponseDTO updateEntityIndicatorProof(DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void deleteEntityIndicatorProof(Integer entityIndicatorId, Integer proofId) {
        var documentFile = documentFileService.findOne(proofId);
        documentFileService.delete(proofId);
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    public void deleteEntityIndicator(Integer entityIndicatorId) {
        // TODO: Do we need any reference constraint checks here
        entityIndicatorRepository.delete(findOne(entityIndicatorId));
    }

    @Override
    protected JpaRepository<EntityIndicator, Integer> getEntityRepository() {
        return entityIndicatorRepository;
    }

    protected void setCommonFields(EntityIndicator entityIndicator,
                                   EntityIndicatorDTO entityIndicatorDTO) {
        if (Objects.nonNull(entityIndicator.getSource()) &&
            !entityIndicator.getSource().equals(EntityIndicatorSource.MANUAL)) {
            throw new CantEditEntityIndicatorException(
                "Only manually entered indicators are editable.");
        }

        entityIndicator.setTimestamp(LocalDateTime.now());
        entityIndicator.setSource(EntityIndicatorSource.MANUAL);

        entityIndicator.setNumericValue(entityIndicatorDTO.getNumericValue());
        entityIndicator.setBooleanValue(entityIndicatorDTO.getBooleanValue());
        entityIndicator.setTextualValue(entityIndicatorDTO.getTextualValue());
        entityIndicator.setFromDate(entityIndicatorDTO.getFromDate());
        entityIndicator.setToDate(entityIndicatorDTO.getToDate());

        entityIndicator.setIndicator(indicatorService.findOne(entityIndicatorDTO.getIndicatorId()));
    }
}
