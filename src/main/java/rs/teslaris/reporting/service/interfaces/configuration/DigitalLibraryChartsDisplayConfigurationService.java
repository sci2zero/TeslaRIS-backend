package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.configuration.DigitalLibraryChartDisplaySettingsDTO;

@Service
public interface DigitalLibraryChartsDisplayConfigurationService {

    DigitalLibraryChartDisplaySettingsDTO getDisplaySettingsForInstitution(
        Integer organisationUnitId);

    void saveDigitalLibraryDisplaySettings(Integer institutionId,
                                           DigitalLibraryChartDisplaySettingsDTO settings);
}
