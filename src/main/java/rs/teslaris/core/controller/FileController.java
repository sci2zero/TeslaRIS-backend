package rs.teslaris.core.controller;

import io.minio.GetObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.statistics.StatisticsService;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.PersonService;
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

    private final PersonService personService;

    private final StatisticsService statisticsIndexService;


    @GetMapping("/{filename}")
    @ResponseBody
    public ResponseEntity<Object> serveFile(
        HttpServletRequest request,
        @PathVariable String filename,
        @RequestHeader(value = "Authorization", required = false) String bearerToken,
        @CookieValue("jwt-security-fingerprint") String fingerprintCookie) throws IOException {

        var file = fileService.loadAsResource(filename);
        var licenseResponse = documentFileService.getDocumentAccessLevel(filename);
        var authenticatedUser = isAuthenticatedUser(bearerToken, fingerprintCookie);

        if (!isOpenAccess(licenseResponse.a) && !authenticatedUser) {
            return ErrorResponseUtil.buildUnavailableResponse(request,
                "loginToViewDocumentMessage");
        }

        if (isOpenAccess(licenseResponse.a) && !authenticatedUser && !licenseResponse.b) {
            return ErrorResponseUtil.buildUnavailableResponse(request,
                "loginToViewCCDocumentMessage");
        }

        if (licenseResponse.a.equals(License.COMMISSION_ONLY) &&
            (!authenticatedUser || !isCommissionUser(bearerToken))) {
            return ErrorResponseUtil.buildUnauthorisedResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        recordDownloadIfApplicable(filename);

        return ResponseEntity.ok()
            .headers(getFileHeaders(file))
            .body(new InputStreamResource(file));
    }

    @GetMapping("/image/{personId}")
    @ResponseBody
    public ResponseEntity<Object> serveImageFile(@PathVariable Integer personId,
                                                 @RequestParam Boolean fullSize)
        throws IOException {
        var person = personService.findOne(personId);

        if (Objects.isNull(person.getProfilePhoto()) ||
            Objects.isNull(person.getProfilePhoto().getProfileImageServerName())) {
            return ResponseEntity.noContent().build();
        }

        var filename = person.getProfilePhoto().getProfileImageServerName();
        var file = fileService.loadAsResource(filename);

        var outputStream = new ByteArrayOutputStream();
        Thumbnails.of(file)
            .size(person.getProfilePhoto().getWidth(), person.getProfilePhoto().getHeight())
            .sourceRegion(person.getProfilePhoto().getLeftOffset(),
                person.getProfilePhoto().getTopOffset(), person.getProfilePhoto().getWidth(),
                person.getProfilePhoto().getHeight()).toOutputStream(outputStream);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Path.of(filename)))
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .body(fullSize ? new InputStreamResource(fileService.loadAsResource(filename)) :
                new ByteArrayResource(outputStream.toByteArray()));
    }

    private boolean isOpenAccess(License license) {
        return license.equals(License.OPEN_ACCESS) || license.equals(License.PUBLIC_DOMAIN);
    }

    private boolean isAuthenticatedUser(String bearerToken, String fingerprintCookie) {
        if (Objects.isNull(bearerToken)) {
            return false;
        }

        var tokenParts = bearerToken.split(" ");
        if (tokenParts.length != 2) {
            return false;
        }

        var token = tokenParts[1];
        var userDetails = userService.loadUserByUsername(tokenUtil.extractUsernameFromToken(token));

        return tokenUtil.validateToken(token, userDetails, fingerprintCookie);
    }

    private boolean isCommissionUser(String bearerToken) {
        var role = tokenUtil.extractUserRoleFromToken(bearerToken);
        return role.equals(UserRole.ADMIN.name()) || role.equals(UserRole.COMMISSION.name());
    }

    private void recordDownloadIfApplicable(String filename) {
        var resourceType = documentFileService.getDocumentResourceType(filename);
        if (resourceType.equals(ResourceType.OFFICIAL_PUBLICATION) ||
            resourceType.equals(ResourceType.PREPRINT)) {
            var documentId = documentFileService.findDocumentIdForFilename(filename);
            if (Objects.nonNull(documentId)) {
                statisticsIndexService.saveDocumentDownload(documentId);
            }
        }
    }

    private HttpHeaders getFileHeaders(GetObjectResponse file) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"));
        headers.set(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"));
        return headers;
    }
}
