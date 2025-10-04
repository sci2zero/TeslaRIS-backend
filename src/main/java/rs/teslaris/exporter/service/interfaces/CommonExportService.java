package rs.teslaris.exporter.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface CommonExportService {

    void exportOrganisationUnitsToCommonModel(boolean allTime);

    void exportPersonsToCommonModel(boolean allTime);

    void exportConferencesToCommonModel(boolean allTime);

    void exportDocumentsToCommonModel(boolean allTime);
}
