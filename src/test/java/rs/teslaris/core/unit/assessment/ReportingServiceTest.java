package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.repository.CommissionReportRepository;
import rs.teslaris.core.assessment.service.impl.ReportingServiceImpl;

@SpringBootTest
public class ReportingServiceTest {

    @Mock
    private CommissionReportRepository commissionReportRepository;

    @InjectMocks
    private ReportingServiceImpl reportingServiceService;


    @Test
    public void shouldGetAvailableReportsForCommission() {
        // Given
        var commissionId = 1;
        var expectedReports = List.of("TABLE_67", "TABLE_63");
        when(commissionReportRepository.getAvailableReportsForCommission(commissionId))
            .thenReturn(expectedReports);

        // When
        var reports =
            reportingServiceService.getAvailableReportsForCommission(commissionId);

        // Then
        assertNotNull(reports);
        assertEquals(expectedReports.size(), reports.size());
        assertEquals(expectedReports, reports);
        verify(commissionReportRepository, times(1)).getAvailableReportsForCommission(commissionId);
    }
}
