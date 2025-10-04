package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.core.io.InputStreamResource;
import rs.teslaris.thesislibrary.dto.ThesisTableExportRequestDTO;

public interface ThesisLibraryTableExportService {

    InputStreamResource exportThesesToCSV(ThesisTableExportRequestDTO request);

    InputStreamResource exportThesesToBibliographicFile(ThesisTableExportRequestDTO request);
}
