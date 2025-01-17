package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.EntityClassificationSource;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;

@Service
public interface PublicationSeriesAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId);

    PublicationSeriesAssessmentClassification createPublicationSeriesAssessmentClassification(
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO);

    void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO);

    void performJournalClassification(Integer commissionId, List<Integer> classificationYears);

    void scheduleClassification(LocalDateTime timeToRun, Integer commissionId, Integer userId,
                                List<Integer> classificationYears);

    void scheduleClassificationLoading(LocalDateTime timeToRun,
                                       EntityClassificationSource source,
                                       Integer userId);
}
