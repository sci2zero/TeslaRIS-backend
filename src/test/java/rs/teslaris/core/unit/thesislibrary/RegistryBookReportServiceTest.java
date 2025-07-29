package rs.teslaris.core.unit.thesislibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.thesislibrary.model.RegistryBookReport;
import rs.teslaris.thesislibrary.repository.RegistryBookReportRepository;
import rs.teslaris.thesislibrary.service.impl.RegistryBookReportServiceImpl;

@SpringBootTest
public class RegistryBookReportServiceTest {

    @Mock
    private RegistryBookReportRepository registryBookReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private FileService fileService;

    @Mock
    private TaskManagerService taskManagerService;

    @InjectMocks
    private RegistryBookReportServiceImpl registryBookReportService;


    @Test
    void shouldListAvailableReportsForUserWithInstitution() {
        // Given
        var userId = 42;
        var rootInstitutionId = 100;
        var subHierarchy = List.of(100, 101);

        var report1 = new RegistryBookReport();
        report1.setReportFileName("report-1.pdf");
        var report2 = new RegistryBookReport();
        report2.setReportFileName("report-2.pdf");

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(rootInstitutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
            rootInstitutionId)).thenReturn(subHierarchy);
        when(registryBookReportRepository.findForInstitution(100)).thenReturn(List.of(report1));
        when(registryBookReportRepository.findForInstitution(101)).thenReturn(List.of(report2));

        // When
        var result = registryBookReportService.listAvailableReports(userId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("report-1.pdf", "report-2.pdf");
    }

    @Test
    void shouldListAllReportsWhenUserHasNoInstitution() {
        // Given
        var userId = 13;
        var report1 = new RegistryBookReport();
        report1.setReportFileName("global-1.pdf");
        var report2 = new RegistryBookReport();
        report2.setReportFileName("global-2.pdf");

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(null);
        when(registryBookReportRepository.findAll()).thenReturn(List.of(report1, report2));

        // When
        var result = registryBookReportService.listAvailableReports(userId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("global-1.pdf", "global-2.pdf");
    }

    @Test
    void shouldServeReportFileForAuthorizedUser() throws IOException {
        // Given
        var userId = 1;
        var fileName = "report.pdf";
        var userInstitutionId = 10;

        var report = mock(RegistryBookReport.class);
        var institution = mock(OrganisationUnit.class);

        when(report.getInstitution()).thenReturn(institution);
        when(institution.getId()).thenReturn(101);

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(userInstitutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitutionId))
            .thenReturn(List.of(101, 102));
        when(registryBookReportRepository.findByReportFileName(fileName))
            .thenReturn(Optional.of(report));

        var response = mock(GetObjectResponse.class);
        when(fileService.loadAsResource(fileName)).thenReturn(response);

        // When
        var result = registryBookReportService.serveReportFile(fileName, userId);

        // Then
        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldThrowWhenUserIsNotAuthorizedToAccessReport() {
        // Given
        var userId = 5;
        var fileName = "secret.pdf";

        var report = mock(RegistryBookReport.class);
        var institution = mock(OrganisationUnit.class);

        when(report.getInstitution()).thenReturn(institution);
        when(institution.getId()).thenReturn(500);

        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(300);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(300))
            .thenReturn(List.of(301, 302));
        when(registryBookReportRepository.findByReportFileName(fileName))
            .thenReturn(Optional.of(report));

        // When & Then
        assertThatThrownBy(() -> registryBookReportService.serveReportFile(fileName, userId))
            .isInstanceOf(LoadingException.class)
            .hasMessageContaining("Unauthorised");
    }

    @Test
    void shouldThrowWhenReportFileDoesNotExist() {
        // Given
        var fileName = "nonexistent.pdf";
        var userId = 1;

        when(registryBookReportRepository.findByReportFileName(fileName)).thenReturn(
            Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registryBookReportService.serveReportFile(fileName, userId))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("No report with given filename.");
    }

    @Test
    void shouldSetCorrectTimeAndSchedulesTask() {
        // Given
        var from = LocalDate.of(2024, 1, 1);
        var to = LocalDate.of(2024, 12, 31);
        var institutionId = 123;
        var lang = "en";
        var userId = 456;

        var mockTime = LocalDateTime.of(2025, 4, 14, 10, 30);

        when(taskManagerService.findNextFreeExecutionTime()).thenReturn(mockTime);

        // When
        String result =
            registryBookReportService.scheduleReportGeneration(from, to, institutionId, lang,
                userId, "", "");

        // Then
        var taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        var idCaptor = ArgumentCaptor.forClass(String.class);

        verify(taskManagerService).scheduleTask(
            idCaptor.capture(),
            eq(mockTime),
            taskCaptor.capture(),
            eq(userId),
            eq(RecurrenceType.ONCE)
        );

        assertTrue(idCaptor.getValue().startsWith("Registry_Book-" + institutionId));
        assertEquals("10:30h", result);
    }

    @Test
    void shouldDeleteWhenReportExists() {
        // Given
        String fileName = "report123.pdf";
        RegistryBookReport mockReport = new RegistryBookReport();
        when(registryBookReportRepository.findByReportFileName(fileName))
            .thenReturn(Optional.of(mockReport));
        when(userRepository.findOrganisationUnitIdForUser(1))
            .thenReturn(0);

        // When
        registryBookReportService.deleteReportFile(fileName, 1);

        // Then
        verify(registryBookReportRepository).delete(mockReport);
    }

    @Test
    void shouldNotDeleteWhenReportNotFound() {
        // Given
        String fileName = "nonexistent.pdf";
        when(registryBookReportRepository.findByReportFileName(fileName))
            .thenReturn(Optional.empty());
        when(userRepository.findOrganisationUnitIdForUser(1))
            .thenReturn(0);

        // When
        registryBookReportService.deleteReportFile(fileName, 1);

        // Then
        verify(registryBookReportRepository, never()).delete(any());
    }
}
