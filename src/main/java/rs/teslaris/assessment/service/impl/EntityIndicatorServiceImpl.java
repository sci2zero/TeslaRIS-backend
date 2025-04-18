package rs.teslaris.assessment.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.dto.EntityIndicatorDTO;
import rs.teslaris.assessment.model.EntityIndicator;
import rs.teslaris.assessment.model.EntityIndicatorSource;
import rs.teslaris.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.EntityIndicatorService;
import rs.teslaris.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EntityIndicatorServiceImpl extends JPAServiceImpl<EntityIndicator> implements
    EntityIndicatorService {

    protected final IndicatorService indicatorService;

    private final EntityIndicatorRepository entityIndicatorRepository;

    private final DocumentFileService documentFileService;


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
        proof.setLicense(License.COMMISSION_ONLY);
        var documentFile = documentFileService.saveNewDocument(proof, false);
        entityIndicator.getProofs().add(documentFile);

        save(entityIndicator);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public DocumentFileResponseDTO updateEntityIndicatorProof(DocumentFileDTO updatedProof) {
        updatedProof.setLicense(License.COMMISSION_ONLY);
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void deleteEntityIndicatorProof(Integer entityIndicatorId, Integer proofId) {
        var documentFile = documentFileService.findOne(proofId);
        var entityIndicator = findOne(entityIndicatorId);
        entityIndicator.getProofs().remove(documentFile);

        documentFileService.delete(proofId);
        save(entityIndicator);
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
            throw new CantEditException(
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
