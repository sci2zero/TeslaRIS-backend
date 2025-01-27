package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;

@Service
public interface DocumentAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId);

    void classifyJournalPublications(LocalDate fromDate);
}
