package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.DocumentExportRequestDTO;
import rs.teslaris.core.dto.commontypes.TableExportRequestDTO;

@Service
public interface TableExportService {

    InputStreamResource exportDocumentsToFile(DocumentExportRequestDTO request);

    InputStreamResource exportPersonsToCSV(TableExportRequestDTO request);

    InputStreamResource exportOrganisationUnitsToCSV(TableExportRequestDTO request);

    Integer getMaxRecordsPerPage();
}
