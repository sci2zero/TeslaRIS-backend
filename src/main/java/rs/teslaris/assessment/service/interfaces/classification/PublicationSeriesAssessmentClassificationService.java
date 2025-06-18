package rs.teslaris.assessment.service.interfaces.classification;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.EntityClassificationSource;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;

@Service
public interface PublicationSeriesAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId);

    PublicationSeriesAssessmentClassification createPublicationSeriesAssessmentClassification(
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO);

    void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO);

    void scheduleClassification(LocalDateTime timeToRun, Integer commissionId, Integer userId,
                                List<Integer> classificationYears, List<Integer> journalIds);

    void scheduleClassificationLoading(LocalDateTime timeToRun,
                                       EntityClassificationSource source,
                                       Integer userId, Integer commissionId);
}
