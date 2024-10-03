package rs.teslaris.core.assessment.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.dto.EntityIndicatorDTO;
import rs.teslaris.core.assessment.model.EntityIndicator;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.EntityIndicatorService;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
@RequiredArgsConstructor
@Transactional
public class EntityIndicatorServiceImpl extends JPAServiceImpl<EntityIndicator> implements
    EntityIndicatorService {

    private final EntityIndicatorRepository entityIndicatorRepository;

    private final DocumentFileService documentFileService;

    private final IndicatorService indicatorService;


    @Override
    public void addEntityIndicatorProof(List<DocumentFileDTO> proofs,
                                        Integer entityIndicatorId) {
        var entityIndicator = findOne(entityIndicatorId);
        proofs.forEach(proof -> {
            var documentFile = documentFileService.saveNewDocument(proof, false);
            entityIndicator.getProofs().add(documentFile);
        });

        save(entityIndicator);
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
        entityIndicator.setTimestamp(LocalDateTime.now());
        entityIndicator.setSource("MANUAL");

        entityIndicator.setNumericValue(entityIndicatorDTO.getNumericValue());
        entityIndicator.setBooleanValue(entityIndicatorDTO.getBooleanValue());
        entityIndicator.setTextualValue(entityIndicatorDTO.getTextualValue());
        entityIndicator.setFromDate(entityIndicatorDTO.getFromDate());
        entityIndicator.setToDate(entityIndicatorDTO.getToDate());

        entityIndicator.setUrls(new HashSet<>(entityIndicatorDTO.getUrls()));

        entityIndicator.setIndicator(indicatorService.findOne(entityIndicatorDTO.getIndicatorId()));
    }
}
