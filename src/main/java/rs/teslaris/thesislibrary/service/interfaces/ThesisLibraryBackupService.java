package rs.teslaris.thesislibrary.service.interfaces;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;

@Service
public interface ThesisLibraryBackupService {

    String scheduleBackupGeneration(Integer institutionId,
                                    LocalDate from, LocalDate to,
                                    List<ThesisType> types,
                                    List<FileSection> documentFileSections,
                                    Boolean defended, Boolean putOnReview,
                                    Integer userId, String language, ExportFileType metadataFormat);

    List<String> listAvailableBackups(Integer userId);

    GetObjectResponse serveAndDeleteBackupFile(String backupFileName, Integer userId)
        throws IOException;
}
