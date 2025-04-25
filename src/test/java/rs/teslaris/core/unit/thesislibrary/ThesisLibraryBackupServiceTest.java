package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import rs.teslaris.core.model.document.DocumentFileBackup;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.DocumentFileBackupRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.thesislibrary.service.impl.ThesisLibraryBackupServiceImpl;

@SpringBootTest
public class ThesisLibraryBackupServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentFileBackupRepository documentFileBackupRepository;

    @Mock
    private TaskManagerService taskManagerService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ThesisLibraryBackupServiceImpl thesisLibraryBackupService;


    @Test
    void shouldScheduleBackupWithCorrectFormatAndReturnTime() {
        // Given
        var institutionId = 1;
        var from = LocalDate.of(2023, 1, 1);
        var to = LocalDate.of(2023, 12, 31);
        var types = List.of(ThesisType.MASTER);
        var fileSections = List.of(FileSection.FILE_ITEMS);
        var defended = true;
        var putOnReview = false;
        var userId = 10;

        var now = LocalDateTime.of(2025, 4, 24, 13, 45);
        when(taskManagerService.findNextFreeExecutionTime()).thenReturn(now);

        // When
        var result =
            thesisLibraryBackupService.scheduleBackupGeneration(institutionId, from, to, types,
                fileSections, defended, putOnReview, userId, "sr");

        // Then
        assertEquals(result, "13:45h");
        verify(taskManagerService).scheduleTask(
            argThat(name -> name.contains("Library_Backup-" + institutionId)),
            eq(now),
            any(Runnable.class),
            eq(userId)
        );
    }

    @Test
    void shouldReturnOnlyAccessibleBackupsForUser() {
        // Given
        var userId = 42;
        var instId = 2;
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(instId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(instId)).thenReturn(
            List.of(instId));
        when(documentFileBackupRepository.findByInstitution(instId)).thenReturn(
            List.of(new DocumentFileBackup(), new DocumentFileBackup()));

        // When
        var result = thesisLibraryBackupService.listAvailableBackups(userId);

        // Then
        assertEquals(result.size(), 2);
    }

    @Test
    void shouldThrowWhenUserIsNotAuthorizedToDownloadBackup() {
        // Given
        var fileName = "backup.zip";
        var userId = 99;

        var report = mock(DocumentFileBackup.class);
        var institution = mock(OrganisationUnit.class);

        when(report.getInstitution()).thenReturn(institution);
        when(institution.getId()).thenReturn(777);
        when(documentFileBackupRepository.findByBackupFileName(fileName)).thenReturn(
            Optional.of(report));
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(100);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(100)).thenReturn(
            List.of(101, 102));

        // When & Then
        assertThrows(LoadingException.class,
            () -> thesisLibraryBackupService.serveAndDeleteBackupFile(fileName, userId));
    }

}
