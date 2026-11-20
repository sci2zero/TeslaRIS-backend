package rs.teslaris.migrator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.repository.MigrationRunRepository;

/**
 * Closes the runs that a shutdown interrupted.
 * <p>
 * A run lives on the thread of the executor and its progress is in memory, so no run survives a
 * restart. Its document, however, keeps the status it had when the application stopped, and nothing
 * else ever comes back to it. The sweep runs before the other startup listeners, so a run that
 * starts right after the application is ready is not mistaken for an interrupted one.
 * <p>
 * A failure is logged and never aborts startup. This listener runs on the startup thread, and an
 * exception raised here leaves {@code SpringApplication.run} and takes the whole application with
 * it, while a stale run status is only a bookkeeping flaw which the next startup sweeps.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigrationRunSweeper {

    private final MigrationRunRepository runRepository;


    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRuns() {
        try {
            var swept = runRepository.failRunningRuns();

            if (swept > 0) {
                log.warn("Marked {} migration run(s) as failed, a restart interrupted them.",
                    swept);
            }
        } catch (DataAccessException e) {
            log.warn("Could not sweep interrupted migration runs, as Mongo is not reachable. " +
                "The next startup tries again.", e);
        }
    }
}
