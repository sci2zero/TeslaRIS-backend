package rs.teslaris.thesislibrary.service.interfaces;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public interface ThesisLibraryBackupService {

    String scheduleBackupGeneration(Integer institutionId,
                                    RelativeDateDTO from, RelativeDateDTO to,
                                    List<ThesisType> types,
                                    List<FileSection> documentFileSections,
                                    Boolean defended, Boolean putOnReview,
                                    Integer userId, String language, ExportFileType metadataFormat,
                                    RecurrenceType recurrence);

    List<String> listAvailableBackups(Integer userId);

    ResponseInputStream<GetObjectResponse> serveBackupFile(String backupFileName, Integer userId)
        throws IOException;

    void deleteBackupFile(String backupFileName);
}
