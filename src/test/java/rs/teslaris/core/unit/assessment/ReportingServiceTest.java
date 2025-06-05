package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.model.CommissionReport;
import rs.teslaris.assessment.repository.CommissionReportRepository;
import rs.teslaris.assessment.service.impl.ReportingServiceImpl;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@SpringBootTest
public class ReportingServiceTest {

    @Mock
    private CommissionReportRepository commissionReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private ReportingServiceImpl reportingServiceService;


    @Test
    public void shouldGetAvailableReportsForCommission() {
        // Given
        var commissionId = 1;
        var expectedReports = List.of("TABLE_67", "TABLE_63");
        when(commissionReportRepository.getAvailableReportsForCommission(commissionId))
            .thenReturn(expectedReports);
        var adminUser = new User();
        adminUser.setAuthority(new Authority(UserRole.ADMIN.name(), new HashSet<>()));
        when(userRepository.findByIdWithOrganisationUnit(1)).thenReturn(Optional.of(adminUser));

        // When
        var reports =
            reportingServiceService.getAvailableReportsForCommission(commissionId, 1);

        // Then
        assertNotNull(reports);
        assertEquals(expectedReports.size(), reports.size());
        assertEquals(expectedReports, reports);
        verify(commissionReportRepository, times(1)).getAvailableReportsForCommission(commissionId);
    }

    @Test
    void shouldReturnAllReportsForAdminUser() {
        // Given
        var userId = 1;
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(null);

        var reports = List.of(
            new CommissionReport(new Commission(), "report1.pdf"),
            new CommissionReport(new Commission(), "report2.pdf")
        );

        when(commissionReportRepository.findAll()).thenReturn(reports);

        // When
        var result = reportingServiceService.getAvailableReportsForUser(userId);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnReportsForNonAdminUser() {
        // Given
        var userId = 2;
        var institutionId = 100;
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(institutionId);

        var subOUs = List.of(100, 101, 102);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(subOUs);

        var commission1 = new Commission();
        commission1.setId(1);
        var commission2 = new Commission();
        commission2.setId(2);

        when(userRepository.findUserCommissionForOrganisationUnit(institutionId))
            .thenReturn(List.of(commission1));
        when(userRepository.findUserCommissionForOrganisationUnit(101))
            .thenReturn(List.of(commission2));
        when(userRepository.findUserCommissionForOrganisationUnit(102))
            .thenReturn(Collections.emptyList());
        when(commissionReportRepository.getAvailableReportsForCommission(1))
            .thenReturn(List.of("reportA.pdf"));
        when(commissionReportRepository.getAvailableReportsForCommission(2))
            .thenReturn(List.of("reportB.pdf", "reportC.pdf"));

        // When
        var result = reportingServiceService.getAvailableReportsForUser(userId);

        // Then
        assertEquals(3, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoReportsAvailable() {
        // Given
        var userId = 3;
        var institutionId = 200;
        when(userRepository.findOrganisationUnitIdForUser(userId)).thenReturn(institutionId);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(Collections.emptyList());

        when(userRepository.findUserCommissionForOrganisationUnit(institutionId))
            .thenReturn(Collections.emptyList());

        // When
        var result = reportingServiceService.getAvailableReportsForUser(userId);

        // Then
        assertTrue(result.isEmpty());
    }
}
