package rs.teslaris.revisioner.service.interfaces;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;

@Service
public interface QualityAssessmentBackfillService {

    void performBackfill(List<QualityAssessmentTarget> targets, List<Integer> personIds,
                         List<Integer> organisationUnitIds, String profileName,
                         boolean rewriteExistingAssessments);

    void scheduleBackfill(Instant timeToRun, List<QualityAssessmentTarget> targets,
                          List<Integer> personIds, List<Integer> organisationUnitIds,
                          String profileName, boolean rewriteExistingAssessments, Integer userId,
                          RecurrenceType recurrence);
}
