package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.model.ReportType;

@Service
public interface ReportingService {

    void generateReport(ReportType type, Integer year, Integer commissionId);
}
