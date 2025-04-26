package rs.teslaris.core.service.interfaces.document;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentFileSection;

@Service
public interface DocumentBackupService {

    String scheduleBackupGeneration(Integer institutionId,
                                    Integer from, Integer to,
                                    List<DocumentPublicationType> types,
                                    List<DocumentFileSection> documentFileSections,
                                    Integer userId, String language);

    List<String> listAvailableBackups(Integer userId);

    GetObjectResponse serveAndDeleteBackupFile(String backupFileName, Integer userId)
        throws IOException;
}
