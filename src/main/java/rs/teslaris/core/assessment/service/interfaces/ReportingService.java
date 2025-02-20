package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.model.ReportType;

@Service
public interface ReportingService {

    void generateReport(ReportType type, Integer year, List<Integer> commissionIds, String locale,
                        Integer topLevelInstitutionId);

    void scheduleReportGeneration(LocalDateTime timeToRun, ReportType reportType,
                                  Integer assessmentYear, List<Integer> commissionIds,
                                  String locale, Integer topLevelInstitutionId, Integer userId);

    List<String> getAvailableReportsForCommission(Integer commissionId);
}
