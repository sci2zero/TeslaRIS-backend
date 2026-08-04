package rs.teslaris.revisioner.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.applicationevent.DataQualityAssessmentReindexEvent;

@Service
public interface DataQualityReindexService {

    void reindexDataQualityAssessments();

    void handleDataQualityAssessmentReindexEvent(DataQualityAssessmentReindexEvent event);
}
