package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;

@RestController
@RequestMapping("/api/csv-export")
@RequiredArgsConstructor
public class CSVExportController {

    private final CSVExportService csvExportService;

    @GetMapping("/records-per-page")
    public Integer getMaxRecordsPerPage() {
        return csvExportService.getMaxRecordsPerPage();
    }

    @PostMapping("/documents")
    public ResponseEntity<InputStreamResource> downloadDocumentCSVExport(@RequestBody @Valid
                                                                         DocumentCSVExportRequest request) {
        var exportDocument = csvExportService.exportDocumentsToCSV(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    @PostMapping("/persons")
    public ResponseEntity<InputStreamResource> downloadPersonCSVExport(
        @RequestBody @Valid CSVExportRequest request) {
        var exportDocument = csvExportService.exportPersonsToCSV(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    @PostMapping("/organisation-units")
    public ResponseEntity<InputStreamResource> downloadOrganisationUnitCSVExport(
        @RequestBody @Valid CSVExportRequest request) {
        var exportDocument = csvExportService.exportOrganisationUnitsToCSV(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    private ResponseEntity<InputStreamResource> serveResponseFile(
        InputStreamResource exportDocument, ExportFileType exportFileType) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export" +
                (exportFileType.equals(ExportFileType.CSV) ? ".csv" : ".xls"))
            .body(exportDocument);
    }
}
