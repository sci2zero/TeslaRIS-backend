package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;

@Service
public interface CSVExportService {

    InputStreamResource exportDocumentsToCSV(DocumentCSVExportRequest request);

    InputStreamResource exportPersonsToCSV(CSVExportRequest request);

    InputStreamResource exportOrganisationUnitsToCSV(CSVExportRequest request);

    Integer getMaxRecordsPerPage();
}
