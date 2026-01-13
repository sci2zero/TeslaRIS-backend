package rs.teslaris.thesislibrary.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
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
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidFileSectionException;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.thesislibrary.model.ThesisFileSection;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryBackupService;

@RestController
@RequestMapping("/api/thesis-library/backup")
@RequiredArgsConstructor
@Traceable
public class ThesisLibraryBackupController {

    private final ThesisLibraryBackupService thesisLibraryBackupService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule-generation")
    @PreAuthorize("hasAuthority('GENERATE_THESIS_LIBRARY_BACKUP')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String scheduleBackupGeneration(@RequestParam RelativeDateDTO from,
                                           @RequestParam RelativeDateDTO to,
                                           @RequestParam Integer institutionId,
                                           @RequestParam List<ThesisType> types,
                                           @RequestParam List<String> sections,
                                           @RequestParam boolean defended,
                                           @RequestParam boolean putOnReview,
                                           @RequestParam String lang,
                                           @RequestParam ExportFileType metadataFormat,
                                           @RequestParam(defaultValue = "ONCE")
                                           RecurrenceType recurrence,
                                           @RequestHeader(value = "Authorization")
                                           String bearerToken) {
        var parsedSections = sections.stream()
            .map(this::parseFileSection)
            .collect(Collectors.toList());

        return thesisLibraryBackupService.scheduleBackupGeneration(institutionId, from, to, types,
            parsedSections, defended, putOnReview, tokenUtil.extractUserIdFromToken(bearerToken),
            lang, metadataFormat, recurrence);
    }

    @GetMapping("/list-backups")
    @PreAuthorize("hasAuthority('GENERATE_THESIS_LIBRARY_BACKUP')")
    public List<String> listBackups(@RequestHeader(value = "Authorization")
                                    String bearerToken) {
        return thesisLibraryBackupService.listAvailableBackups(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/download/{backupFileName}")
    @PreAuthorize("hasAuthority('GENERATE_THESIS_LIBRARY_BACKUP')")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> serveAndDeleteBackupFile(
        HttpServletRequest request, @PathVariable String backupFileName,
        @RequestHeader(value = "Authorization") String bearerToken) throws IOException {
        if (!SessionUtil.isSessionValid(request, bearerToken) ||
            !SessionUtil.hasAnyRole(bearerToken,
                List.of(UserRole.ADMIN, UserRole.INSTITUTIONAL_LIBRARIAN,
                    UserRole.HEAD_OF_LIBRARY))) {
            return ErrorResponseUtil.buildUnauthorisedStreamingResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        var file = thesisLibraryBackupService.serveBackupFile(backupFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
        Runnable deleteCallback = () -> thesisLibraryBackupService.deleteBackupFile(backupFileName);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(file.headers().get("Content-Disposition")))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .header(HttpHeaders.CONTENT_LENGTH, file.headers().get("Content-Length"))
            .body(StreamingUtil.createStreamingBody(file, deleteCallback));
    }

    private FileSection parseFileSection(String input) {
        try {
            return ThesisFileSection.valueOf(input);
        } catch (IllegalArgumentException e1) {
            try {
                return DocumentFileSection.valueOf(input);
            } catch (IllegalArgumentException e2) {
                throw new InvalidFileSectionException("Invalid file section value: " + input);
            }
        }
    }
}
