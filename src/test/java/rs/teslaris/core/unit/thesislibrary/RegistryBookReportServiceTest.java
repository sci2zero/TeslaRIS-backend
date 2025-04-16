package rs.teslaris.core.unit.thesislibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.RegistryBookException;
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
            .isInstanceOf(RegistryBookException.class)
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
            .isInstanceOf(RegistryBookException.class)
            .hasMessageContaining("No report with given filename.");
    }
}
