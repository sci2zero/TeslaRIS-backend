package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;

@RestController
@RequestMapping("/api/csv-export")
@RequiredArgsConstructor
public class CSVExportController {

    private final CSVExportService csvExportService;

    @PostMapping("/documents")
    public ResponseEntity<InputStreamResource> downloadDocumentCSVExport(@RequestBody @Valid
                                                                         DocumentCSVExportRequest request) {
        var document = csvExportService.exportDocumentsToCSV(request);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.csv")
            .body(document);
    }
}
