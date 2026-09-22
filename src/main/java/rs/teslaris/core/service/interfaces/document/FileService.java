package rs.teslaris.core.service.interfaces.document;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.util.functional.Pair;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public interface FileService {

    String store(MultipartFile file, String serverFilename);

    String store(Resource resource, String serverFilename, String originalFilename);

    void delete(String serverFilename);

    ResponseInputStream<GetObjectResponse> loadAsResource(String serverFilename) throws IOException;

    Pair<String, InputStream> duplicateFile(String serverFilename);
}
