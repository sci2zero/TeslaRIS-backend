package rs.teslaris.revisioner.service.interfaces;

import jakarta.annotation.Nullable;
import java.time.Instant;
import rs.teslaris.core.model.commontypes.RecurrenceType;

public interface DataQualityFixatorRunService {

    void runPipeline(String pipelineName, Integer userId);

    void runAllPipelines(Integer userId);

    void scheduleFixatorRun(Instant timeToRun, @Nullable String pipelineName, Integer userId,
                            RecurrenceType recurrence);
}
