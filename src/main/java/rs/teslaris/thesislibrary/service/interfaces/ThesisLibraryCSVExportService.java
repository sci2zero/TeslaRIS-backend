package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.core.io.InputStreamResource;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;

public interface ThesisLibraryCSVExportService {

    InputStreamResource exportThesesToCSV(ThesisCSVExportRequestDTO request);

    InputStreamResource exportThesesToBibliographicFile(ThesisCSVExportRequestDTO request);
}
