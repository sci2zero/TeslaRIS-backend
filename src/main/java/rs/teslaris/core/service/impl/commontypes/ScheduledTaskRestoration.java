package rs.teslaris.core.service.impl.commontypes;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

/**
 * Restores the stored tasks of one row at a time, which the restorers of every module share.
 */
@Component
public class ScheduledTaskRestoration {

    private final TaskManagerService taskManagerService;

    private final TransactionTemplate transactionTemplate;


    public ScheduledTaskRestoration(TaskManagerService taskManagerService,
                                    PlatformTransactionManager transactionManager) {
        this.taskManagerService = taskManagerService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // Each row restores on its own, so a row that fails rolls back no other row.
        this.transactionTemplate.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }


    /**
     * Restores one row in a transaction of its own, and drops the tasks of the row from the
     * schedule if that transaction does not commit.
     */
    public void restoreOne(Runnable restore) {
        transactionTemplate.executeWithoutResult(status -> {
            // Registered before the restore, which is what fails when a row rolls back.
            dropNewTasksOnRollback(scheduledTaskIds());

            restore.run();
        });
    }

    private void dropNewTasksOnRollback(Set<String> scheduledBefore) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (STATUS_COMMITTED == status) {
                    return;
                }

                // A task of the rolled back row keeps no row of its own, so it does not survive
                // a restart.
                scheduledTaskIds().stream()
                    .filter(taskId -> !scheduledBefore.contains(taskId))
                    .forEach(taskManagerService::dropScheduledTask);
            }
        });
    }

    private Set<String> scheduledTaskIds() {
        return taskManagerService.listScheduledTasks().stream()
            .map(ScheduledTaskResponseDTO::taskId)
            .collect(Collectors.toSet());
    }
}
