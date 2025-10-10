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
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.OUChartsDisplayConfigurationService;

@Service
@Transactional
public class OUChartsDisplayConfigurationServiceImpl
    extends BaseChartsDisplayConfigurationServiceImpl
    implements OUChartsDisplayConfigurationService {

    @Autowired
    public OUChartsDisplayConfigurationServiceImpl(
        ChartsDisplayConfigurationRepository chartsDisplayConfigurationRepository,
        OrganisationUnitService organisationUnitService) {
        super(chartsDisplayConfigurationRepository, organisationUnitService);
    }

    @Override
    public OUChartDisplaySettingsDTO getDisplaySettingsForOrganisationUnit(
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

        var trueConfiguration = new OUChartDisplaySettingsDTO(
            deduceBaseConfigurationFormMultipleSources(configurations,
                ChartsDisplayConfiguration::getOuChartDisplaySettings));

        trueConfiguration.setPublicationCountPersonLeaderboard(
            createChartSetting(configurations, "publicationCountPersonLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setPublicationCountSubUnitLeaderboard(
            createChartSetting(configurations, "publicationCountSubUnitLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setCitationCountPersonLeaderboard(
            createChartSetting(configurations, "citationCountPersonLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setCitationCountSubUnitLeaderboard(
            createChartSetting(configurations, "citationCountSubUnitLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setAssessmentPointPersonLeaderboard(
            createChartSetting(configurations, "assessmentPointCountPersonLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setAssessmentPointSubUnitLeaderboard(
            createChartSetting(configurations, "assessmentPointCountSubUnitLeaderboard",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));

        return trueConfiguration;
    }

    @Override
    public void saveOrganisationUnitDisplaySettings(Integer institutionId,
                                                    OUChartDisplaySettingsDTO settings) {
        var existingConfiguration =
            chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                institutionId);
        var configuration =
            existingConfiguration.orElseGet(() -> createNewConfiguration(institutionId));

        if (Objects.isNull(configuration.getOuChartDisplaySettings())) {
            configuration.setOuChartDisplaySettings(new HashMap<>());
        }

        setBaseConfigurationFields(configuration.getOuChartDisplaySettings(), settings);

        configuration.getOuChartDisplaySettings()
            .put("publicationCountPersonLeaderboard",
                settings.getPublicationCountPersonLeaderboard());
        configuration.getOuChartDisplaySettings()
            .put("publicationCountSubUnitLeaderboard",
                settings.getPublicationCountSubUnitLeaderboard());
        configuration.getOuChartDisplaySettings()
            .put("citationCountPersonLeaderboard", settings.getCitationCountPersonLeaderboard());
        configuration.getOuChartDisplaySettings()
            .put("citationCountSubUnitLeaderboard", settings.getCitationCountSubUnitLeaderboard());
        configuration.getOuChartDisplaySettings()
            .put("assessmentPointCountPersonLeaderboard",
                settings.getAssessmentPointPersonLeaderboard());
        configuration.getOuChartDisplaySettings()
            .put("assessmentPointCountSubUnitLeaderboard",
                settings.getAssessmentPointSubUnitLeaderboard());

        save(configuration);
    }

    private void addDefaultConfiguration(List<ChartsDisplayConfiguration> configurations) {
        var configuration = new ChartsDisplayConfiguration();

        setDefaultConfigurationValues(configuration.getOuChartDisplaySettings());
        setOUConfigurationSpecificDefaultFields(configuration.getOuChartDisplaySettings());

        configurations.add(configuration);
    }
}
