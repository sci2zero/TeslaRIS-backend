package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.DocumentAssessmentClassificationDTO;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.ImaginaryJournalPublicationAssessmentResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Service
public interface DocumentAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId);

    EntityAssessmentClassificationResponseDTO createDocumentAssessmentClassification(
        DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO);

    void editDocumentAssessmentClassification(Integer classificationId,
                                              DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO);

    void classifyJournalPublication(Integer journalPublicationId);

    void classifyProceedingsPublication(Integer proceedingsPublicationId);

    void schedulePublicationClassification(LocalDateTime timeToRun,
                                           Integer userId, LocalDate fromDate,
                                           DocumentPublicationType documentPublicationType,
                                           Integer commissionId,
                                           List<Integer> authorIds,
                                           List<Integer> orgUnitIds,
                                           List<Integer> journalIds);

    ImaginaryJournalPublicationAssessmentResponseDTO assessImaginaryJournalPublication(
        Integer journalId, Integer commissionId,
        Integer classificationYear, String researchArea,
        Integer authorCount, boolean isExperimental, boolean isTheoretical, boolean isSimulation);
}
