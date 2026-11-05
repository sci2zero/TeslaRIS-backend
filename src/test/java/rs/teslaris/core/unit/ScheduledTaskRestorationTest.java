package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.service.impl.commontypes.ScheduledTaskRestoration;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskRestorationTest {

    private static final ScheduledTaskResponseDTO RESTORED_TASK = new ScheduledTaskResponseDTO(
        "Migration_Sync-hydrator-PERSON-restored", Instant.now(), RecurrenceType.DAILY);

    @Mock
    private TaskManagerService taskManagerService;

    private ScheduledTaskRestoration taskRestoration;

    @BeforeEach
    public void setUp() {
        taskRestoration = new ScheduledTaskRestoration(taskManagerService,
            new StubTransactionManager());
    }

    @Test
    public void shouldDropTheTasksOfARowThatDoesNotCommit() {
        // given
        when(taskManagerService.listScheduledTasks())
            .thenReturn(List.of(), List.of(RESTORED_TASK));

        // when
        assertThrows(IllegalStateException.class, () -> taskRestoration.restoreOne(() -> {
            throw new IllegalStateException("row is broken");
        }));

        // then
        verify(taskManagerService).dropScheduledTask(RESTORED_TASK.taskId());
    }

    @Test
    public void shouldKeepTheTasksOfARowThatCommits() {
        // given
        when(taskManagerService.listScheduledTasks())
            .thenReturn(List.of(), List.of(RESTORED_TASK));

        // when
        taskRestoration.restoreOne(() -> {
        });

        // then
        verify(taskManagerService, never()).dropScheduledTask(RESTORED_TASK.taskId());
    }

    /**
     * Activates the synchronization of a real transaction, over no resource at all.
     */
    private static class StubTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
