package rs.teslaris.exporter.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;

@RestController
@RequestMapping("/api/ro-crate")
@RequiredArgsConstructor
public class RoCrateExportController {

    private final RoCrateExportService roCrateExportService;


    @GetMapping("/document/{documentId}")
    public ResponseEntity<StreamingResponseBody> downloadRoCrate(@PathVariable Integer documentId,
                                                                 @RequestParam String exportId)
        throws IOException {
        var zipPath =
            roCrateExportService.createRoCrateZip(
                documentId, exportId);

        long contentLength = Files.size(zipPath);

        StreamingResponseBody body = outputStream -> {
            try (InputStream in =
                     Files.newInputStream(zipPath)) {
                in.transferTo(outputStream);
            } finally {
                Files.deleteIfExists(zipPath);
            }
        };

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(
                    "attachment; filename=\"ro-crate-"
                        + documentId + ".zip\""))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(contentLength)
            .body(body);
    }

    @GetMapping("/person/{personId}")
    @PersonEditCheck
    public ResponseEntity<StreamingResponseBody> downloadRoCrateBibliography(
        @PathVariable Integer personId, @RequestParam String exportId) throws IOException {
        var zipPath = roCrateExportService.createRoCrateBibliographyZip(personId, exportId);

        long contentLength = Files.size(zipPath);

        StreamingResponseBody body = outputStream -> {
            try (InputStream in =
                     Files.newInputStream(zipPath)) {
                in.transferTo(outputStream);
            } finally {
                Files.deleteIfExists(zipPath);
            }
        };

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(
                    "attachment; filename=\"ro-crate-"
                        + personId + ".zip\""))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(contentLength)
            .body(body);
    }
}
