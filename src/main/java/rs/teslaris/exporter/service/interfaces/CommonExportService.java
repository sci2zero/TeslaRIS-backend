package rs.teslaris.exporter.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.exporter.model.common.ExportPublicationType;

@Service
public interface CommonExportService {

    void exportOrganisationUnitsToCommonModel(boolean allTime);

    void exportPersonsToCommonModel(boolean allTime);

    void exportConferencesToCommonModel(boolean allTime);

    void exportDocumentsToCommonModel(boolean allTime, List<ExportPublicationType> exportTypes);
}
