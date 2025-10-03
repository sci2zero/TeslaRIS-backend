package rs.teslaris.core.service.interfaces.document;

import io.minio.GetObjectResponse;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {

    String store(MultipartFile file, String serverFilename);

    void delete(String serverFilename);

    GetObjectResponse loadAsResource(String serverFilename) throws IOException;

    String duplicateFile(String serverFilename);
}
