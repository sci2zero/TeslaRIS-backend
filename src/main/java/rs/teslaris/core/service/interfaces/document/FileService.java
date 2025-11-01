package rs.teslaris.core.service.interfaces.document;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.util.functional.Pair;

@Service
public interface FileService {

    String store(MultipartFile file, String serverFilename);

    void delete(String serverFilename);

    GetObjectResponse loadAsResource(String serverFilename) throws IOException;

    Pair<String, InputStream> duplicateFile(String serverFilename);
}
