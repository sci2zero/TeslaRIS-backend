package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Service
public interface DocumentAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId);

    void classifyJournalPublication(Integer journalPublicationId);

    void classifyProceedingsPublication(Integer proceedingsPublicationId);

    void schedulePublicationClassification(LocalDateTime timeToRun,
                                           Integer userId, LocalDate fromDate,
                                           DocumentPublicationType documentPublicationType,
                                           Integer commissionId,
                                           List<Integer> authorIds,
                                           List<Integer> orgUnitIds,
                                           List<Integer> journalIds);
}
