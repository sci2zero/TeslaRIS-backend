package rs.teslaris.core.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {

    String store(MultipartFile file);

    String detectMimeType(MultipartFile file);

    Resource loadAsResource(String serverFilename);
}
