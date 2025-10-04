package rs.teslaris.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentBackupService;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.session.SessionUtil;

@RestController
@RequestMapping("/api/document/backup")
@RequiredArgsConstructor
@Traceable
public class DocumentBackupController {

    private final DocumentBackupService documentBackupService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule-generation")
    @PreAuthorize("hasAuthority('GENERATE_OUTPUT_BACKUP')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String scheduleBackupGeneration(@RequestParam Integer from,
                                           @RequestParam Integer to,
                                           @RequestParam Integer institutionId,
                                           @RequestParam List<DocumentPublicationType> types,
                                           @RequestParam List<DocumentFileSection> sections,
                                           @RequestParam String lang,
                                           @RequestParam ExportFileType metadataFormat,
                                           @RequestParam(defaultValue = "ONCE")
                                           RecurrenceType recurrence,
                                           @RequestHeader(value = "Authorization")
                                           String bearerToken) {
        return documentBackupService.scheduleBackupGeneration(institutionId, from,
            to, types, sections, tokenUtil.extractUserIdFromToken(bearerToken), lang,
            metadataFormat, recurrence);
    }

    @GetMapping("/list-backups")
    @PreAuthorize("hasAuthority('GENERATE_OUTPUT_BACKUP')")
    public List<String> listBackups(@RequestHeader(value = "Authorization")
                                    String bearerToken) {
        return documentBackupService.listAvailableBackups(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/download/{backupFileName}")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> serveAndDeleteBackupFile(
        HttpServletRequest request,
        @PathVariable String backupFileName,
        @RequestHeader(value = "Authorization")
        String bearerToken) throws IOException {
        if (!SessionUtil.isSessionValid(request, bearerToken) ||
            !SessionUtil.hasAnyRole(bearerToken,
                List.of(UserRole.ADMIN, UserRole.INSTITUTIONAL_EDITOR))) {
            return ErrorResponseUtil.buildUnauthorisedStreamingResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        var file = documentBackupService.serveBackupFile(backupFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
        Runnable deleteCallback = () -> documentBackupService.deleteBackupFile(backupFileName);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, "application/zip")
            .header(HttpHeaders.CONTENT_LENGTH, file.headers().get("Content-Length"))
            .body(StreamingUtil.createStreamingBody(file, deleteCallback));
    }
}
