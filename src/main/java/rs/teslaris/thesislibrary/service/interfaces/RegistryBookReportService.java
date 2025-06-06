package rs.teslaris.thesislibrary.service.interfaces;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface RegistryBookReportService {

    String scheduleReportGeneration(LocalDate from, LocalDate to, Integer institutionId,
                                    String lang, Integer userId, String authorName,
                                    String authorTitle);

    List<String> listAvailableReports(Integer userId);

    GetObjectResponse serveReportFile(String reportFileName, Integer userId) throws IOException;

    void deleteReportFile(String reportFileName, Integer userId);
}
