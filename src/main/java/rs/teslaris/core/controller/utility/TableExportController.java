package rs.teslaris.core.controller.utility;

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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.DocumentExportRequestDTO;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.TableExportRequestDTO;
import rs.teslaris.core.service.interfaces.commontypes.TableExportService;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/table-export")
@RequiredArgsConstructor
@Traceable
public class TableExportController {

    private final TableExportService tableExportService;

    @GetMapping("/records-per-page")
    public Integer getMaxRecordsPerPage() {
        return tableExportService.getMaxRecordsPerPage();
    }

    @PostMapping("/documents")
    public ResponseEntity<InputStreamResource> downloadDocumentCSVExport(@RequestBody @Valid
                                                                         DocumentExportRequestDTO request) {
        var exportDocument = tableExportService.exportDocumentsToFile(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    @PostMapping("/persons")
    public ResponseEntity<InputStreamResource> downloadPersonCSVExport(
        @RequestBody @Valid TableExportRequestDTO request) {
        var exportDocument = tableExportService.exportPersonsToCSV(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    @PostMapping("/organisation-units")
    public ResponseEntity<InputStreamResource> downloadOrganisationUnitCSVExport(
        @RequestBody @Valid TableExportRequestDTO request) {
        var exportDocument = tableExportService.exportOrganisationUnitsToCSV(request);
        return serveResponseFile(exportDocument, request.getExportFileType());
    }

    private ResponseEntity<InputStreamResource> serveResponseFile(
        InputStreamResource exportDocument, ExportFileType exportFileType) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(
                    "attachment; filename=export" + exportFileType.getValue()))
            .body(exportDocument);
    }
}
