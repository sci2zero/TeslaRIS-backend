package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.configuration.FullChartDisplaySettingsDTO;

@Service
public interface BaseChartsDisplayConfigurationService {

    FullChartDisplaySettingsDTO getSavedConfigurationForOU(Integer organisationUnitId);
}
