package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;

@Service
public interface PersonChartsDisplayConfigurationService
    extends JPAService<ChartsDisplayConfiguration> {

    PersonChartDisplaySettingsDTO getDisplaySettingsForPerson(Integer personId);

    void savePersonDisplaySettings(Integer institutionId, PersonChartDisplaySettingsDTO settings);
}
