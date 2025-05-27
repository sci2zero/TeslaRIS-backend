package rs.teslaris.core.controller;

import java.io.IOException;
import java.util.List;
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
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.service.interfaces.document.DocumentBackupService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/document/backup")
@RequiredArgsConstructor
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
                                           @RequestHeader(value = "Authorization")
                                           String bearerToken) {
        return documentBackupService.scheduleBackupGeneration(institutionId, from,
            to, types, sections, tokenUtil.extractUserIdFromToken(bearerToken), lang,
            metadataFormat);
    }

    @GetMapping("/list-backups")
    @PreAuthorize("hasAuthority('GENERATE_OUTPUT_BACKUP')")
    public List<String> listBackups(@RequestHeader(value = "Authorization")
                                    String bearerToken) {
        return documentBackupService.listAvailableBackups(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/download/{backupFileName}")
    @PreAuthorize("hasAuthority('GENERATE_OUTPUT_BACKUP')")
    @ResponseBody
    public ResponseEntity<Object> serveAndDeleteBackupFile(@PathVariable String backupFileName,
                                                           @RequestHeader(value = "Authorization")
                                                           String bearerToken) throws IOException {
        var file = documentBackupService.serveAndDeleteBackupFile(backupFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .body(new InputStreamResource(file));
    }
}
