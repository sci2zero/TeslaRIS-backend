package rs.teslaris.migrator.service.interfaces;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.migrator.dto.MigrationFailureDTO;
import rs.teslaris.migrator.dto.MigrationRequestDTO;
import rs.teslaris.migrator.dto.MigrationRunResponseDTO;
import rs.teslaris.migrator.util.MigrationEntityType;

@Service
public interface MigrationService {

    /**
     * Starts a run and returns immediately with its id. Execution happens on the migration executor.
     */
    String startRun(MigrationRequestDTO request);

    MigrationRunResponseDTO getRun(String runId);

    List<MigrationRunResponseDTO> listRuns(String source, MigrationEntityType entityType,
                                           Pageable pageable);

    List<MigrationFailureDTO> getFailures(String runId, Pageable pageable);

    /**
     * Clears the failed entries of a finished run and starts a new one over the same source, in
     * resume mode - items that already succeeded are skipped, failed ones are attempted again.
     */
    String retryFailed(String runId, Integer triggeredByUserId);

    List<String> listRegisteredPipelines();
}
