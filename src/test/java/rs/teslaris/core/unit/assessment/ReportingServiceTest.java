package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.repository.CommissionReportRepository;
import rs.teslaris.core.assessment.service.impl.ReportingServiceImpl;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.UserRepository;

@SpringBootTest
public class ReportingServiceTest {

    @Mock
    private CommissionReportRepository commissionReportRepository;

    @Mock
    private UserRepository userRepository;

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
}
