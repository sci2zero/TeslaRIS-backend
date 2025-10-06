package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.reporting.dto.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.PersonChartsDisplayConfiguration;

@Service
public interface PersonChartsDisplayConfigurationService
    extends JPAService<PersonChartsDisplayConfiguration> {

    PersonChartDisplaySettingsDTO getDisplaySettingsForPerson(Integer personId);

    void savePersonDisplaySettings(Integer institutionId, PersonChartDisplaySettingsDTO settings);
}
