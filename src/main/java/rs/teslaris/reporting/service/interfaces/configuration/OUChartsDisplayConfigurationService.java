package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;

@Service
public interface OUChartsDisplayConfigurationService {

    OUChartDisplaySettingsDTO getDisplaySettingsForOrganisationUnit(Integer organisationUnitId);

    void saveOrganisationUnitDisplaySettings(Integer institutionId,
                                             OUChartDisplaySettingsDTO settings);
}
