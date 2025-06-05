package rs.teslaris.thesislibrary.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidFileSectionException;
import rs.teslaris.core.util.jwt.JwtUtil;
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
    public String scheduleBackupGeneration(@RequestParam LocalDate from,
                                           @RequestParam LocalDate to,
                                           @RequestParam Integer institutionId,
                                           @RequestParam List<ThesisType> types,
                                           @RequestParam List<String> sections,
                                           @RequestParam boolean defended,
                                           @RequestParam boolean putOnReview,
                                           @RequestParam String lang,
                                           @RequestParam ExportFileType metadataFormat,
                                           @RequestHeader(value = "Authorization")
                                           String bearerToken) {
        var parsedSections = sections.stream()
            .map(this::parseFileSection)
            .collect(Collectors.toList());

        return thesisLibraryBackupService.scheduleBackupGeneration(institutionId, from, to, types,
            parsedSections, defended, putOnReview, tokenUtil.extractUserIdFromToken(bearerToken),
            lang, metadataFormat);
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
    public ResponseEntity<Object> serveAndDeleteBackupFile(@PathVariable String backupFileName,
                                                           @RequestHeader(value = "Authorization")
                                                           String bearerToken) throws IOException {
        var file = thesisLibraryBackupService.serveAndDeleteBackupFile(backupFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .body(new InputStreamResource(file));
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
