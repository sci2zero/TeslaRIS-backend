package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentFileBackup;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.DocumentFileBackupRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.document.DocumentBackupServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.BackupException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@SpringBootTest
class DocumentBackupServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private DocumentRepository documentRepository;

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
    private DocumentBackupServiceImpl documentBackupService;

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldScheduleBackupWithCorrectFormatAndReturnTime(ExportFileType metadataFormat) {
        // Given
        var institutionId = 1;
        var from = 2020;
        var to = 2023;
        var types = List.of(DocumentPublicationType.MONOGRAPH);
        var fileSections = List.of(DocumentFileSection.FILE_ITEMS);
        var userId = 5;
        var language = "en";

        var now = LocalDateTime.of(2025, 4, 26, 14, 30);
        when(taskManagerService.findNextFreeExecutionTime()).thenReturn(now);

        // When
        var result = documentBackupService.scheduleBackupGeneration(
            institutionId, from, to, types, fileSections, userId, language, metadataFormat
        );

        // Then
        assertEquals("14:30h", result);
        verify(taskManagerService).scheduleTask(
            argThat(name -> name.contains("Document_Backup-" + institutionId)),
            eq(now),
            any(Runnable.class),
            eq(userId)
        );
    }

    @Test
    void shouldListAvailableBackupsWhenUserHasInstitution() {
        // Given
        var userId = 10;
        var institutionId = 2;
        var backupFileName = "backup.zip";

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(institutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId));
        when(documentFileBackupRepository.findByInstitution(institutionId))
            .thenReturn(List.of(new DocumentFileBackup() {{
                setBackupFileName(backupFileName);
            }}));

        // When
        var result = documentBackupService.listAvailableBackups(userId);

        // Then
        assertEquals(List.of(backupFileName), result);
    }

    @Test
    void shouldListAvailableBackupsWhenUserHasNoInstitution() {
        // Given
        var userId = 10;
        var backupFileName = "backup.zip";

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(null);
        when(documentFileBackupRepository.findAll())
            .thenReturn(List.of(new DocumentFileBackup() {{
                setBackupFileName(backupFileName);
            }}));

        // When
        var result = documentBackupService.listAvailableBackups(userId);

        // Then
        assertEquals(List.of(backupFileName), result);
    }

    @Test
    void shouldServeAndDeleteBackupFileWhenAuthorized() throws IOException {
        // Given
        var userId = 10;
        var institutionId = 2;
        var backupFileName = "backup.zip";

        var documentBackup = new DocumentFileBackup();
        var institution = new OrganisationUnit();
        institution.setId(institutionId);
        documentBackup.setInstitution(institution);

        when(documentFileBackupRepository.findByBackupFileName(backupFileName))
            .thenReturn(Optional.of(documentBackup));
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(institutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId));
        var resource = mock(GetObjectResponse.class);
        when(fileService.loadAsResource(backupFileName)).thenReturn(resource);

        // When
        var result = documentBackupService.serveAndDeleteBackupFile(backupFileName, userId);

        // Then
        assertEquals(resource, result);
        verify(fileService).delete(backupFileName);
        verify(documentFileBackupRepository).delete(documentBackup);
    }

    @Test
    void shouldThrowWhenServeAndDeleteBackupFileUnauthorized() {
        // Given
        var userId = 10;
        var institutionId = 2;
        var otherInstitutionId = 3;
        var backupFileName = "backup.zip";

        var documentBackup = new DocumentFileBackup();
        var institution = new OrganisationUnit();
        institution.setId(otherInstitutionId);
        documentBackup.setInstitution(institution);

        when(documentFileBackupRepository.findByBackupFileName(backupFileName))
            .thenReturn(Optional.of(documentBackup));
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(institutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(institutionId)); // Not containing otherInstitutionId

        // When / Then
        assertThrows(LoadingException.class,
            () -> documentBackupService.serveAndDeleteBackupFile(backupFileName, userId));
    }

    @ParameterizedTest
    @EnumSource(ExportFileType.class)
    void shouldThrowWhenScheduleBackupWithInvalidDates(ExportFileType metadataFormat) {
        // Given
        var institutionId = 1;
        var from = 2025;
        var to = 2024;
        var types = List.of(DocumentPublicationType.MONOGRAPH);
        var fileSections = List.of(DocumentFileSection.FILE_ITEMS);
        var userId = 5;
        var language = "en";

        // When & Then
        assertThrows(BackupException.class,
            () -> documentBackupService.scheduleBackupGeneration(
                institutionId, from, to, types, fileSections, userId, language, metadataFormat
            ));
    }
}

