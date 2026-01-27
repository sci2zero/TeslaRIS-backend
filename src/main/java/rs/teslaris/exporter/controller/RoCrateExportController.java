package rs.teslaris.exporter.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.AuthenticationException;
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
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;

@RestController
@RequestMapping("/api/ro-crate")
@RequiredArgsConstructor
public class RoCrateExportController {

    private final RoCrateExportService roCrateExportService;


    @GetMapping("/document/{documentId}")
    public ResponseEntity<StreamingResponseBody> downloadRoCrate(
        @PathVariable Integer documentId,
        @RequestParam String exportId) throws AuthenticationException {
        if (!SessionUtil.isUserLoggedIn()) {
            throw new LoadingException(
                "You have to log in to be able to download Ro-Crates.");
        }

        var byteArrayOutputStream = new ByteArrayOutputStream();
        roCrateExportService.createRoCrateZip(documentId, exportId, byteArrayOutputStream);

        var bytes = byteArrayOutputStream.toByteArray();
        StreamingResponseBody body = StreamingUtil.createStreamingBody(
            new ByteArrayInputStream(bytes));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"ro-crate-" + documentId + ".zip\"")
            .header(HttpHeaders.CONTENT_TYPE, "application/zip")
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(body);
    }

    @GetMapping("/person/{personId}")
    @PersonEditCheck
    public ResponseEntity<StreamingResponseBody> downloadRoCrateBibliography(
        @PathVariable Integer personId,
        @RequestParam String exportId) {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        roCrateExportService.createRoCrateBibliographyZip(personId, exportId,
            byteArrayOutputStream);

        var bytes = byteArrayOutputStream.toByteArray();
        StreamingResponseBody body = StreamingUtil.createStreamingBody(
            new ByteArrayInputStream(bytes));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"ro-crate-" + personId + ".zip\"")
            .header(HttpHeaders.CONTENT_TYPE, "application/zip")
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(body);
    }
}
