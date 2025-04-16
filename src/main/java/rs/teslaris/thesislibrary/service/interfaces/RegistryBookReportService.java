package rs.teslaris.thesislibrary.service.interfaces;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

@Service
public interface RegistryBookReportService {

    InputStreamResource generateReport(LocalDate from, LocalDate to, Integer institutionId,
                                       String lang);

    List<String> listAvailableReports(Integer userId);

    GetObjectResponse serveReportFile(String reportFileName, Integer userId) throws IOException;
}
