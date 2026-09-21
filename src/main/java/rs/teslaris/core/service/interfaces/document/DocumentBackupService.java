package rs.teslaris.core.service.interfaces.document;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.DocumentFileSection;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public interface DocumentBackupService {

    String scheduleBackupGeneration(Integer institutionId,
                                    Integer from, Integer to,
                                    List<DocumentPublicationType> types,
                                    List<DocumentFileSection> documentFileSections,
                                    Integer userId, String language, ExportFileType metadataFormat,
                                    RecurrenceType recurrence);

    List<String> listAvailableBackups(Integer userId);

    ResponseInputStream<GetObjectResponse> serveBackupFile(String backupFileName, Integer userId)
        throws IOException;

    void deleteBackupFile(String backupFileName);
}
