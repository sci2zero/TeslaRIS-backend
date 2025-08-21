package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.DocumentExportRequestDTO;
import rs.teslaris.core.dto.commontypes.TableExportRequest;

@Service
public interface CSVExportService {

    InputStreamResource exportDocumentsToFile(DocumentExportRequestDTO request);

    InputStreamResource exportPersonsToCSV(TableExportRequest request);

    InputStreamResource exportOrganisationUnitsToCSV(TableExportRequest request);

    Integer getMaxRecordsPerPage();
}
