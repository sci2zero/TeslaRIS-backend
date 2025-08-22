package rs.teslaris.core.controller;

import io.minio.GetObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.hibernate.Hibernate;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentDownloadTracker;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
@Traceable
public class FileController {

    private final FileService fileService;

    private final DocumentFileService documentFileService;

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentDownloadTracker documentDownloadTracker;


    @GetMapping("/{filename}")
    @ResponseBody
    public ResponseEntity<Object> serveFile(
        HttpServletRequest request,
        @PathVariable String filename,
        @RequestParam(value = "inline", defaultValue = "false") Boolean inline,
        @RequestHeader(value = "Authorization", required = false) String bearerToken,
        @CookieValue("jwt-security-fingerprint") String fingerprintCookie) throws IOException {

        var file = fileService.loadAsResource(filename);
        var documentFile = documentFileService.getDocumentByServerFilename(filename);
        var accessRights = documentFile.getAccessRights();
        var isVerifiedDocument = documentFile.getIsVerifiedData();
        var authenticatedUser = isAuthenticatedUser(bearerToken, fingerprintCookie);
        var isOpenAccess = isOpenAccess(accessRights);
        var isThesisDocument = Objects.nonNull(documentFile.getDocument()) &&
            Hibernate.getClass(documentFile.getDocument()).equals(Thesis.class);

        if (isThesisDocument && ((Thesis) documentFile.getDocument()).getIsOnPublicReview()) {
            return serveFile(filename, documentFile, file, inline);
        }

        if (!isOpenAccess && !authenticatedUser) {
            return ErrorResponseUtil.buildUnavailableResponse(request,
                "loginToViewDocumentMessage");
        }

        if (isOpenAccess && !authenticatedUser && !isVerifiedDocument) {
            return ErrorResponseUtil.buildUnavailableResponse(request,
                "loginToViewCCDocumentMessage");
        }

        if (accessRights.equals(AccessRights.COMMISSION_ONLY) &&
            (!authenticatedUser || !isCommissionUser(bearerToken))) {
            return handleUnauthorisedUser(request);
        }

        if (!isOpenAccess) {
            var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(bearerToken));
            var userId = tokenUtil.extractUserIdFromToken(bearerToken);

            if (Objects.nonNull(documentFile.getPerson())) {
                var personId = documentFile.getPerson().getId();
                switch (role) {
                    case ADMIN:
                        break;
                    case RESEARCHER:
                        if (!userService.isUserAResearcher(userId, personId)) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    case INSTITUTIONAL_EDITOR:
                        if (!personService.isPersonEmployedInOrganisationUnit(personId,
                            userService.getUserOrganisationUnitId(userId))) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    default:
                        return handleUnauthorisedUser(request);
                }
            } else if (Objects.nonNull(documentFile.getDocument())) {
                var document = documentFile.getDocument();
                var contributors = document.getContributors().stream()
                    .filter(contribution -> Objects.nonNull(contribution.getPerson()))
                    .map(contribution -> contribution.getPerson().getId())
                    .collect(Collectors.toSet());

                switch (role) {
                    case ADMIN:
                        break;
                    case RESEARCHER:
                        var personId = userService.getPersonIdForUser(userId);
                        if (!contributors.contains(personId)) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    case INSTITUTIONAL_EDITOR:
                        if (noResearchersFromUserInstitution(contributors, userId) &&
                            isDocumentNotAThesis(userId, document)) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    case INSTITUTIONAL_LIBRARIAN:
                        if (isDocumentNotAThesis(userId, document)) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    case HEAD_OF_LIBRARY:
                        if (noResearchersFromUserInstitution(contributors, userId)) {
                            return handleUnauthorisedUser(request);
                        }
                        break;
                    default:
                        return handleUnauthorisedUser(request);
                }
            }
        }

        return serveFile(filename, documentFile, file, inline);
    }

    private ResponseEntity<Object> serveFile(String filename, DocumentFile documentFile,
                                             GetObjectResponse file, Boolean inline)
        throws IOException {
        recordDownloadIfApplicable(filename, documentFile.getResourceType());

        byte[] fileBytes = file.readAllBytes();
        var headers = getFileHeaders(file, inline, fileBytes);
        FairSignpostingL1Utility.addHeadersForDocumentFileItems(headers, documentFile);

        return ResponseEntity.ok()
            .headers(headers)
            .body(fileBytes);
    }

    private ResponseEntity<Object> handleUnauthorisedUser(HttpServletRequest request) {
        return ErrorResponseUtil.buildUnauthorisedResponse(request,
            "unauthorisedToViewDocumentMessage");
    }

    private boolean isDocumentNotAThesis(Integer userId, Document document) {
        var userInstitutionId = userService.getUserOrganisationUnitId(userId);
        var institutionSubUnitIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitutionId);

        return !(document instanceof Thesis) ||
            !institutionSubUnitIds.contains(((Thesis) document).getOrganisationUnit().getId());
    }

    private boolean noResearchersFromUserInstitution(Set<Integer> contributors,
                                                     Integer userId) {
        return contributors.stream()
            .filter(contributorId -> contributorId > 0) // filter out external affiliates
            .noneMatch(
                contributorId -> personService.isPersonEmployedInOrganisationUnit(
                    contributorId,
                    userService.getUserOrganisationUnitId(userId)));
    }

    @GetMapping("/image/{personId}")
    @ResponseBody
    public ResponseEntity<Object> serveImageFile(@PathVariable Integer personId,
                                                 @RequestParam Boolean fullSize)
        throws IOException {
        var person = personService.findOne(personId);

        if (Objects.isNull(person.getProfilePhoto()) ||
            Objects.isNull(person.getProfilePhoto().getImageServerName())) {
            return ResponseEntity.noContent().build();
        }

        var filename = person.getProfilePhoto().getImageServerName();
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

    @GetMapping("/logo/{organisationUnitId}")
    @ResponseBody
    public ResponseEntity<Object> serveLogoFile(@PathVariable Integer organisationUnitId,
                                                @RequestParam Boolean fullSize)
        throws IOException {
        var organisationUnit = organisationUnitService.findOne(organisationUnitId);

        if (Objects.isNull(organisationUnit.getLogo()) ||
            Objects.isNull(organisationUnit.getLogo().getImageServerName())) {
            return ResponseEntity.noContent().build();
        }

        var filename = organisationUnit.getLogo().getImageServerName();
        var file = fileService.loadAsResource(filename);

        var outputStream = new ByteArrayOutputStream();
        var croppedResized = Thumbnails.of(file)
            .size(organisationUnit.getLogo().getWidth(),
                organisationUnit.getLogo().getHeight())
            .sourceRegion(organisationUnit.getLogo().getLeftOffset(),
                organisationUnit.getLogo().getTopOffset(),
                organisationUnit.getLogo().getWidth(),
                organisationUnit.getLogo().getHeight()).asBufferedImage();

        int cropWidth = organisationUnit.getLogo().getWidth();
        int cropHeight = organisationUnit.getLogo().getHeight();

        int canvasSize = Math.max(cropWidth, cropHeight);
        int x = (canvasSize - cropWidth) / 2;
        int y = (canvasSize - cropHeight) / 2;

        var canvas = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = canvas.createGraphics();
        if (Objects.nonNull(organisationUnit.getLogo().getBackgroundHex())) {
            g2d.setColor(Color.decode(organisationUnit.getLogo().getBackgroundHex()));
        } else {
            g2d.setColor(Color.decode("#a8b2bd"));
        }

        g2d.fillRect(0, 0, canvasSize, canvasSize);
        g2d.drawImage(croppedResized, x, y, null);
        g2d.dispose();

        ImageIO.write(canvas, "png", outputStream);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Path.of(filename)))
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .body(fullSize ? new InputStreamResource(fileService.loadAsResource(filename)) :
                new ByteArrayResource(outputStream.toByteArray()));
    }

    private boolean isOpenAccess(AccessRights accessRights) {
        return accessRights.equals(AccessRights.OPEN_ACCESS);
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

    private void recordDownloadIfApplicable(String filename, ResourceType resourceType) {
        if (resourceType.equals(ResourceType.OFFICIAL_PUBLICATION) ||
            resourceType.equals(ResourceType.PREPRINT)) {
            var documentId = documentFileService.findDocumentIdForFilename(filename);
            if (Objects.nonNull(documentId)) {
                documentDownloadTracker.saveDocumentDownload(documentId);
            }
        }
    }

    private HttpHeaders getFileHeaders(GetObjectResponse file, Boolean inline, byte[] fileBytes) {
        HttpHeaders headers = new HttpHeaders();

        var contentDisposition = file.headers().get("Content-Disposition");
        if (Objects.nonNull(contentDisposition) && inline) {
            contentDisposition = contentDisposition.replace("attachment", "inline");
            var tika = new Tika();
            try {
                String detectedType = tika.detect(new ByteArrayInputStream(fileBytes));
                headers.set(HttpHeaders.CONTENT_TYPE, detectedType);
            } catch (IOException e) {
                log.error(
                    "FileController - failed to detect MIME type for: {} - Proceeding with default octet-stream.",
                    contentDisposition);
                headers.set(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            }
        } else {
            headers.set(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"));
        }

        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        return headers;
    }
}
