package rs.teslaris.exporter.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface CommonExportService {

    void exportOrganisationUnitsToCommonModel();

    void exportPersonsToCommonModel();

    void exportConferencesToCommonModel();

    void exportDocumentsToCommonModel();
}
