package rs.teslaris.core.exporter.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface CommonExportService {

    void exportOrganisationUnitsToCommonModel();

    void exportPersonsToCommonModel();

    void exportConferencesToCommonModel();

    void exportDocumentsToCommonModel();
}
