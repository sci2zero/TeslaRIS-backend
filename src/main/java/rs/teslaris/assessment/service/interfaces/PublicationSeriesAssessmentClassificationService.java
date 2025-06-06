package rs.teslaris.assessment.service.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.assessment.model.EntityClassificationSource;
import rs.teslaris.assessment.model.PublicationSeriesAssessmentClassification;

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
