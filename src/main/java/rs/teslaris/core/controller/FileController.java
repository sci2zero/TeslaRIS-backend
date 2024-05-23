package rs.teslaris.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.ErrorObject;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    private final DocumentFileService documentFileService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{filename}")
    @ResponseBody
    public ResponseEntity<Object> serveFile(HttpServletRequest request,
                                            @PathVariable String filename,
                                            @RequestHeader(value = "Authorization", required = false)
                                            String bearerToken) throws IOException {
        var file = fileService.loadAsResource(filename);

        var license = documentFileService.getDocumentAccessLevel(filename);

        if (!license.equals(License.OPEN_ACCESS) && !license.equals(License.PUBLIC_DOMAIN)) {
            if (Objects.isNull(bearerToken)) {
                return buildUnavailableResponse(request, "loginToViewDocumentMessage");
            }

            var tokenParts = bearerToken.split(" ");
            if (tokenParts.length != 2) {
                return buildUnauthorisedResponse(request, "unauthorisedToViewDocumentMessage");
            }

            var token = bearerToken.split(" ")[1];

            var userDetails =
                userService.loadUserByUsername(tokenUtil.extractUsernameFromToken(token));

            if (!tokenUtil.validateToken(token, userDetails)) {
                return buildUnauthorisedResponse(request, "unauthorisedToViewDocumentMessage");
            }
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"")
            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Path.of(filename)))
            .body(file);
    }

    private ResponseEntity<Object> buildUnauthorisedResponse(HttpServletRequest request,
                                                             String message) {
        return ResponseEntity.status(401)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new ErrorObject(request, message,
                HttpStatus.UNAUTHORIZED));
    }

    private ResponseEntity<Object> buildUnavailableResponse(HttpServletRequest request,
                                                            String message) {
        return ResponseEntity.status(451)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new ErrorObject(request, message,
                HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS));
    }
}
