package rs.teslaris.reporting.service.impl.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.DigitalLibraryChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.DigitalLibraryChartsDisplayConfigurationService;

@Service
@Transactional
public class DigitalLibraryChartsDisplayConfigurationServiceImpl
    extends BaseChartsDisplayConfigurationServiceImpl
    implements DigitalLibraryChartsDisplayConfigurationService {

    @Autowired
    public DigitalLibraryChartsDisplayConfigurationServiceImpl(
        ChartsDisplayConfigurationRepository chartsDisplayConfigurationRepository,
        OrganisationUnitService organisationUnitService) {
        super(chartsDisplayConfigurationRepository, organisationUnitService);
    }

    @Override
    public DigitalLibraryChartDisplaySettingsDTO getDisplaySettingsForInstitution(
        Integer organisationUnitId) {
        var institutionIds = new ArrayList<>(List.of(organisationUnitId));
        institutionIds.addAll(
            organisationUnitService.getSuperOUsHierarchyRecursive(organisationUnitId));

        var configurations = new ArrayList<ChartsDisplayConfiguration>();

        for (var institutionId : institutionIds) {
            var configuration =
                chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                    institutionId);

            if (configuration.isPresent()) {
                configurations.add(configuration.get());
                break;
            }
        }

        if (configurations.isEmpty()) {
            addDefaultConfiguration(configurations);
        }

        var trueConfiguration = new DigitalLibraryChartDisplaySettingsDTO();

        trueConfiguration.setThesisCountTotal(
            createChartSetting(configurations, "thesisCountTotal",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisCountByYear(
            createChartSetting(configurations, "thesisCountByYear",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisTypeByYear(
            createChartSetting(configurations, "thesisTypeByYear",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisTypeRatio(
            createChartSetting(configurations, "thesisTypeRatio",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisViewCountTotal(
            createChartSetting(configurations, "thesisViewCountTotal",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisViewCountByMonth(
            createChartSetting(configurations, "thesisViewCountByMonth",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisViewCountByCountry(
            createChartSetting(configurations, "thesisViewCountByCountry",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisDownloadCountTotal(
            createChartSetting(configurations, "thesisDownloadCountTotal",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisDownloadCountByMonth(
            createChartSetting(configurations, "thesisDownloadCountByMonth",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setThesisDownloadCountByCountry(
            createChartSetting(configurations, "thesisDownloadCountByCountry",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setViewCountThesisLeaderboard(
            createChartSetting(configurations, "viewCountThesisLeaderboard",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));
        trueConfiguration.setDownloadCountThesisLeaderboard(
            createChartSetting(configurations, "downloadCountThesisLeaderboard",
                ChartsDisplayConfiguration::getDigitalLibraryChartDisplaySettings));

        return trueConfiguration;
    }

    @Override
    public void saveDigitalLibraryDisplaySettings(Integer institutionId,
                                                  DigitalLibraryChartDisplaySettingsDTO settings) {
        var existingConfiguration =
            chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                institutionId);
        var configuration =
            existingConfiguration.orElseGet(() -> createNewConfiguration(institutionId));

        if (Objects.isNull(configuration.getDigitalLibraryChartDisplaySettings())) {
            configuration.setDigitalLibraryChartDisplaySettings(new HashMap<>());
        }

        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisCountTotal",
                settings.getThesisCountTotal());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisCountByYear",
                settings.getThesisCountByYear());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisTypeByYear", settings.getThesisTypeByYear());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisTypeRatio", settings.getThesisTypeRatio());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisViewCountTotal",
                settings.getThesisViewCountTotal());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisViewCountByMonth",
                settings.getThesisViewCountByMonth());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisViewCountByCountry",
                settings.getThesisViewCountByCountry());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisDownloadCountTotal",
                settings.getThesisDownloadCountTotal());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisDownloadCountByMonth",
                settings.getThesisDownloadCountByMonth());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("thesisDownloadCountByCountry",
                settings.getThesisDownloadCountByCountry());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("viewCountThesisLeaderboard",
                settings.getViewCountThesisLeaderboard());
        configuration.getDigitalLibraryChartDisplaySettings()
            .put("downloadCountThesisLeaderboard",
                settings.getDownloadCountThesisLeaderboard());

        save(configuration);
    }

    private void addDefaultConfiguration(List<ChartsDisplayConfiguration> configurations) {
        var configuration = new ChartsDisplayConfiguration();

        setDigitalLibraryConfigurationSpecificDefaultFields(
            configuration.getDigitalLibraryChartDisplaySettings());

        configurations.add(configuration);
    }
}
