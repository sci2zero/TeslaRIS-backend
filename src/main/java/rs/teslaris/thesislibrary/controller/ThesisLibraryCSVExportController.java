package rs.teslaris.thesislibrary.controller;

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
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryCSVExportService;

@RestController
@RequestMapping("/api/thesis-library/csv-export")
@RequiredArgsConstructor
public class ThesisLibraryCSVExportController {

    private final ThesisLibraryCSVExportService thesisLibraryCSVExportService;

    @PostMapping
    public ResponseEntity<InputStreamResource> downloadThesisLibraryCSVExport(
        @RequestBody @Valid ThesisCSVExportRequestDTO request) {
        var exportDocument = thesisLibraryCSVExportService.exportThesesToCSV(request);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export" +
                (request.getExportFileType().equals(ExportFileType.CSV) ? ".csv" : ".xls"))
            .body(exportDocument);
    }
}
