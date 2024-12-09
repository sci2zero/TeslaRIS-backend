package rs.teslaris.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.service.interfaces.statistics.StatisticsService;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    private final DocumentFileService documentFileService;

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final StatisticsService statisticsIndexService;


    @GetMapping("/{filename}")
    @ResponseBody
    public ResponseEntity<Object> serveFile(HttpServletRequest request,
                                            @PathVariable String filename,
                                            @RequestHeader(value = "Authorization", required = false)
                                            String bearerToken,
                                            @CookieValue("jwt-security-fingerprint")
                                            String fingerprintCookie) throws IOException {
        var file = fileService.loadAsResource(filename);

        var license = documentFileService.getDocumentAccessLevel(filename);

        if (!license.equals(License.OPEN_ACCESS) && !license.equals(License.PUBLIC_DOMAIN)) {
            if (Objects.isNull(bearerToken)) {
                return ErrorResponseUtil.buildUnavailableResponse(request,
                    "loginToViewDocumentMessage");
            }

            var tokenParts = bearerToken.split(" ");
            if (tokenParts.length != 2) {
                return ErrorResponseUtil.buildUnauthorisedResponse(request,
                    "unauthorisedToViewDocumentMessage");
            }

            var token = bearerToken.split(" ")[1];

            var userDetails =
                userService.loadUserByUsername(tokenUtil.extractUsernameFromToken(token));

            if (!tokenUtil.validateToken(token, userDetails, fingerprintCookie)) {
                return ErrorResponseUtil.buildUnauthorisedResponse(request,
                    "unauthorisedToViewDocumentMessage");
            }
        }

        var resourceType = documentFileService.getDocumentResourceType(filename);
        if (resourceType.equals(ResourceType.OFFICIAL_PUBLICATION) ||
            resourceType.equals(ResourceType.PREPRINT)) {
            var documentId = documentFileService.findDocumentIdForFilename(filename);
            if (Objects.nonNull(documentId)) {
                statisticsIndexService.saveDocumentDownload(documentId);
            }
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"")
            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Path.of(filename)))
            .body(file);
    }
}
