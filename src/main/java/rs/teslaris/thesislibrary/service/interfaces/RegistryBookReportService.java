package rs.teslaris.thesislibrary.service.interfaces;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public interface RegistryBookReportService {

    String scheduleReportGeneration(RelativeDateDTO from, RelativeDateDTO to, Integer institutionId,
                                    String lang, Integer userId, String authorName,
                                    String authorTitle, RecurrenceType recurrence);

    List<String> listAvailableReports(Integer userId);

    ResponseInputStream<GetObjectResponse> serveReportFile(String reportFileName, Integer userId)
        throws IOException;

    void deleteReportFile(String reportFileName, Integer userId);
}
