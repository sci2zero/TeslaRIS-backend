package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;

@Service
public interface DocumentAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId);

    void classifyJournalPublication(Integer journalPublicationId);

    void scheduleJournalPublicationClassification(LocalDateTime timeToRun,
                                                  Integer userId, LocalDate fromDate,
                                                  Integer commissionId,
                                                  ArrayList<Integer> authorIds,
                                                  ArrayList<Integer> orgUnitIds,
                                                  ArrayList<Integer> journalIds);
}
