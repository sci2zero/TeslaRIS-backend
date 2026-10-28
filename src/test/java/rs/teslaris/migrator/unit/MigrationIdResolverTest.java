package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

@ExtendWith(MockitoExtension.class)
public class MigrationIdResolverTest {

    @Mock
    private MigrationRecordLogRepository recordLogRepository;

    @InjectMocks
    private MigrationIdResolver idResolver;


    @Test
    public void shouldReturnIdOfMigratedEntity() {
        // given
        when(recordLogRepository.findTargetEntityId("test-source", MigrationEntityType.PERSON,
            "cv-1")).thenReturn(Optional.of(5));

        // when
        var id = idResolver.require("test-source", MigrationEntityType.PERSON, "cv-1");

        // then
        assertEquals(5, id);
    }

    @Test
    public void shouldNameTheMissingPassWhenEntityIsNotMigrated() {
        // given
        when(recordLogRepository.findTargetEntityId("test-source", MigrationEntityType.PROJECT,
            "TEST999")).thenReturn(Optional.empty());

        // when
        var exception = assertThrows(MigrationException.class,
            () -> idResolver.require("test-source", MigrationEntityType.PROJECT, "TEST999"));

        // then
        assertEquals("PROJECT 'TEST999' has not been migrated yet - run the PROJECT pass first.",
            exception.getMessage());
    }

    @Test
    public void shouldRememberEntityCreatedAsPartOfAnother() {
        // when
        idResolver.remember("test-source", MigrationEntityType.PROJECT_ORGANISATION,
            "TEST001#consortium#2", 11, "run-1");

        // then
        var captor = ArgumentCaptor.forClass(MigrationRecordLog.class);
        verify(recordLogRepository).record(captor.capture());
        var entry = captor.getValue();
        assertEquals("test-source", entry.getSource());
        assertEquals(MigrationEntityType.PROJECT_ORGANISATION, entry.getEntityType());
        assertEquals("TEST001#consortium#2", entry.getSourceKey());
        assertEquals(MigrationItemStatus.CREATED, entry.getStatus());
        assertEquals(11, entry.getTargetEntityId());
        assertEquals("run-1", entry.getRunId());
    }
}
