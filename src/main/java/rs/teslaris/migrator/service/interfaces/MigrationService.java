package rs.teslaris.migrator.service.interfaces;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.migrator.dto.MigrationFailureDTO;
import rs.teslaris.migrator.dto.MigrationPipelineResponseDTO;
import rs.teslaris.migrator.dto.MigrationRequestDTO;
import rs.teslaris.migrator.dto.MigrationRunResponseDTO;
import rs.teslaris.migrator.dto.MigrationScheduleRequestDTO;
import rs.teslaris.migrator.util.MigrationEntityType;

@Service
public interface MigrationService {

    /**
     * Starts a run and returns immediately with its id. Execution happens on the migration executor.
     */
    String startRun(MigrationRequestDTO request);

    /**
     * Runs on the calling thread and returns once the run is over. Meant for a scheduled task.
     */
    String runSynchronously(MigrationRequestDTO request);

    /**
     * Schedules a run over the entity type of the request. Every firing reads the window of
     * {@code modifiedAfterDays} days before it, so a missed firing needs no catching up.
     */
    String scheduleRun(Instant timeToRun, MigrationScheduleRequestDTO request,
                       RecurrenceType recurrence);

    MigrationRunResponseDTO getRun(String runId);

    Page<MigrationRunResponseDTO> listRuns(String source, MigrationEntityType entityType,
                                           Pageable pageable);

    Page<MigrationFailureDTO> getFailures(String runId, Pageable pageable);

    /**
     * Clears the failed entries of a finished run and starts a new one over the same source, in
     * resume mode - items that already succeeded are skipped, failed ones are attempted again.
     */
    String retryFailed(String runId, Integer triggeredByUserId);

    /**
     * Every registered pipeline, with the batch size it uses by default and whether it runs now.
     */
    List<MigrationPipelineResponseDTO> listRegisteredPipelines();
}
