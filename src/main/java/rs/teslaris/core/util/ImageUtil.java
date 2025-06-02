package rs.teslaris.core.util;

import java.io.IOException;
import java.util.List;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

public class ImageUtil {

    public static boolean isMIMETypeInvalid(MultipartFile multipartFile, boolean isLogo)
        throws IOException {
        if (multipartFile.isEmpty()) {
            return false;
        }

        List<String> validMimeTypes;
        if (isLogo) {
            validMimeTypes = List.of("image/png");
        } else {
            validMimeTypes = List.of("image/jpeg", "image/png");
        }

        String contentType = multipartFile.getContentType();
        if (!validMimeTypes.contains(contentType)) {
            return true;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null ||
            !(originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") ||
                originalFilename.endsWith(".png"))) {
            return true;
        }

        var tika = new Tika();
        String detectedType = tika.detect(multipartFile.getInputStream());
        return !validMimeTypes.contains(detectedType);
    }
}
