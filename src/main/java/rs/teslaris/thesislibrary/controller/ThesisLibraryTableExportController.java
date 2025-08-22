package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.thesislibrary.dto.ThesisTableExportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryTableExportService;

@RestController
@RequestMapping("/api/thesis-library/table-export")
@RequiredArgsConstructor
@Traceable
public class ThesisLibraryTableExportController {

    private final ThesisLibraryTableExportService thesisLibraryTableExportService;

    @PostMapping
    public ResponseEntity<InputStreamResource> downloadThesisLibraryCSVExport(
        @RequestBody @Valid ThesisTableExportRequestDTO request) {

        InputStreamResource exportDocument;
        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            exportDocument = thesisLibraryTableExportService.exportThesesToCSV(request);
        } else {
            exportDocument =
                thesisLibraryTableExportService.exportThesesToBibliographicFile(request);
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=export" + request.getExportFileType().getValue())
            .body(exportDocument);
    }
}
