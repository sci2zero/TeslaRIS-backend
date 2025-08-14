package rs.teslaris.assessment.service.interfaces;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.ReportDTO;
import rs.teslaris.assessment.model.ReportType;
import rs.teslaris.core.model.commontypes.RecurrenceType;

@Service
public interface ReportingService {

    void generateReport(ReportType type, Integer year, List<Integer> commissionIds, String locale,
                        Integer topLevelInstitutionId);

    void scheduleReportGeneration(LocalDateTime timeToRun, ReportType reportType,
                                  Integer assessmentYear, List<Integer> commissionIds,
                                  String locale, Integer topLevelInstitutionId, Integer userId,
                                  RecurrenceType recurrence);

    List<String> getAvailableReportsForCommission(Integer commissionId, Integer userId);

    List<ReportDTO> getAvailableReportsForUser(Integer userId);

    GetObjectResponse serveReportFile(String reportName, Integer userId, Integer commissionId)
        throws IOException;
}
