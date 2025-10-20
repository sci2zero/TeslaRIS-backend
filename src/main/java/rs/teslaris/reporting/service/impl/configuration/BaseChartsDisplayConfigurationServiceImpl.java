package rs.teslaris.reporting.service.impl.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.BaseChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.DigitalLibraryChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.FullChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.BaseChartsDisplayConfigurationService;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class BaseChartsDisplayConfigurationServiceImpl
    extends JPAServiceImpl<ChartsDisplayConfiguration> implements
    BaseChartsDisplayConfigurationService {

    protected final ChartsDisplayConfigurationRepository
        chartsDisplayConfigurationRepository;

    protected final OrganisationUnitService organisationUnitService;


    @Override
    protected JpaRepository<ChartsDisplayConfiguration, Integer> getEntityRepository() {
        return chartsDisplayConfigurationRepository;
    }

    protected void setBaseConfigurationFields(Map<String, ChartDisplaySettings> configuration,
                                              BaseChartDisplaySettingsDTO settings) {
        configuration
            .put("publicationCountTotal", settings.getPublicationCountTotal());
        configuration
            .put("publicationCountByYear", settings.getPublicationCountByYear());
        configuration
            .put("publicationTypeByYear", settings.getPublicationTypeByYear());
        configuration
            .put("publicationCategoryByYear", settings.getPublicationCategoryByYear());
        configuration
            .put("publicationTypeRatio", settings.getPublicationTypeRatio());
        configuration
            .put("publicationCategoryRatio", settings.getPublicationCategoryRatio());
        configuration
            .put("citationCountTotal", settings.getPublicationTypeRatio());
        configuration
            .put("citationCountByYear", settings.getPublicationCategoryRatio());
        configuration
            .put("viewCountTotal", settings.getViewCountTotal());
        configuration
            .put("viewCountByMonth", settings.getViewCountByMonth());
        configuration
            .put("viewCountByCountry", settings.getViewCountByCountry());
    }

    protected void setDefaultConfigurationValues(Map<String, ChartDisplaySettings> configuration) {
        configuration
            .put("publicationCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("publicationCountByYear", new ChartDisplaySettings(true, false));
        configuration
            .put("publicationTypeByYear", new ChartDisplaySettings(true, false));
        configuration
            .put("publicationCategoryByYear", new ChartDisplaySettings(true, true));
        configuration
            .put("publicationTypeRatio", new ChartDisplaySettings(true, false));
        configuration
            .put("publicationCategoryRatio", new ChartDisplaySettings(true, true));
        configuration
            .put("citationCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("citationCountByYear", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountByMonth", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountByCountry", new ChartDisplaySettings(true, true));
    }

    protected void setOUConfigurationSpecificDefaultFields(
        Map<String, ChartDisplaySettings> configuration) {
        configuration
            .put("publicationCountPersonLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("publicationCountSubUnitLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("citationCountPersonLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("citationCountSubUnitLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("assessmentPointCountPersonLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("assessmentPointCountSubUnitLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountPersonLeaderboard", new ChartDisplaySettings(true, true));
        configuration
            .put("viewCountDocumentLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("downloadCountDocumentLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("citationCountDocumentLeaderboard", new ChartDisplaySettings(true, false));
    }

    protected void setDigitalLibraryConfigurationSpecificDefaultFields(
        Map<String, ChartDisplaySettings> configuration) {
        configuration
            .put("thesisCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisCountByYear", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisTypeByYear", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisTypeRatio", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisViewCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisViewCountByMonth", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisViewCountByCountry", new ChartDisplaySettings(true, true));
        configuration
            .put("thesisDownloadCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisDownloadCountByMonth", new ChartDisplaySettings(true, false));
        configuration
            .put("thesisDownloadCountByCountry", new ChartDisplaySettings(true, true));
        configuration
            .put("viewCountThesisLeaderboard", new ChartDisplaySettings(true, false));
        configuration
            .put("downloadCountThesisLeaderboard", new ChartDisplaySettings(true, false));
    }

    protected void setPersonConfigurationSpecificDefaultFields(
        Map<String, ChartDisplaySettings> configuration) {
        // Empty for now, kept for easier maintenance and scaling
    }

    protected void setDocumentConfigurationSpecificDefaultFields(
        Map<String, ChartDisplaySettings> configuration) {
        configuration
            .put("viewCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountByMonth", new ChartDisplaySettings(true, false));
        configuration
            .put("downloadCountTotal", new ChartDisplaySettings(true, false));
        configuration
            .put("downloadCountByMonth", new ChartDisplaySettings(true, false));
        configuration
            .put("viewCountByCountry", new ChartDisplaySettings(true, true));
        configuration
            .put("downloadCountByCountry", new ChartDisplaySettings(true, true));
    }

    protected ChartDisplaySettings createChartSetting(
        List<ChartsDisplayConfiguration> configurations,
        String chartKey,
        Function<ChartsDisplayConfiguration, Map<String, ChartDisplaySettings>> settingsExtractor) {

        boolean display = configurations.stream()
            .anyMatch(conf -> settingsExtractor.apply(conf)
                .getOrDefault(chartKey, new ChartDisplaySettings(true, true))
                .getDisplay());

        boolean spanWholeRow = configurations.stream()
            .anyMatch(conf -> settingsExtractor.apply(conf)
                .getOrDefault(chartKey, new ChartDisplaySettings(true, true))
                .getSpanWholeRow());

        return new ChartDisplaySettings(display, spanWholeRow);
    }

    protected BaseChartDisplaySettingsDTO deduceBaseConfigurationFormMultipleSources(
        List<ChartsDisplayConfiguration> configurations,
        Function<ChartsDisplayConfiguration, Map<String, ChartDisplaySettings>> settingsExtractor) {
        return new BaseChartDisplaySettingsDTO(
            createChartSetting(configurations, "publicationCountTotal", settingsExtractor),
            createChartSetting(configurations, "publicationCountByYear", settingsExtractor),
            createChartSetting(configurations, "publicationTypeByYear", settingsExtractor),
            createChartSetting(configurations, "publicationCategoryByYear", settingsExtractor),
            createChartSetting(configurations, "publicationTypeRatio", settingsExtractor),
            createChartSetting(configurations, "publicationCategoryRatio", settingsExtractor),
            createChartSetting(configurations, "citationCountTotal", settingsExtractor),
            createChartSetting(configurations, "citationCountByYear", settingsExtractor),
            createChartSetting(configurations, "viewCountTotal", settingsExtractor),
            createChartSetting(configurations, "viewCountByMonth", settingsExtractor),
            createChartSetting(configurations, "viewCountByCountry", settingsExtractor)
        );
    }

    @Override
    public FullChartDisplaySettingsDTO getSavedConfigurationForOU(Integer organisationUnitId) {
        var savedConfiguration =
            chartsDisplayConfigurationRepository.getConfigurationForInstitution(organisationUnitId);

        ChartsDisplayConfiguration configuration;
        if (savedConfiguration.isPresent()) {
            configuration = savedConfiguration.get();
        } else {
            configuration = new ChartsDisplayConfiguration();
            setDefaultConfigurationValues(configuration.getPersonChartDisplaySettings());
            setPersonConfigurationSpecificDefaultFields(
                configuration.getPersonChartDisplaySettings());

            setDefaultConfigurationValues(configuration.getOuChartDisplaySettings());
            setOUConfigurationSpecificDefaultFields(configuration.getOuChartDisplaySettings());

            setDocumentConfigurationSpecificDefaultFields(
                configuration.getDocumentChartDisplaySettings());
        }

        var response = new FullChartDisplaySettingsDTO();
        var personSettings = configuration.getPersonChartDisplaySettings();
        response.setPersonChartDisplaySettings(new PersonChartDisplaySettingsDTO(
            personSettings.get("publicationCountTotal"),
            personSettings.get("publicationCountByYear"),
            personSettings.get("publicationTypeByYear"),
            personSettings.get("publicationCategoryByYear"),
            personSettings.get("publicationTypeRatio"),
            personSettings.get("publicationCategoryRatio"),
            personSettings.get("citationCountTotal"),
            personSettings.get("citationCountByYear"),
            personSettings.get("viewCountTotal"),
            personSettings.get("viewCountByMonth"),
            personSettings.get("viewCountByCountry")
        ));

        var ouSettings = configuration.getOuChartDisplaySettings();
        response.setOuChartDisplaySettings(new OUChartDisplaySettingsDTO(
            ouSettings.get("publicationCountTotal"),
            ouSettings.get("publicationCountByYear"),
            ouSettings.get("publicationTypeByYear"),
            ouSettings.get("publicationCategoryByYear"),
            ouSettings.get("publicationTypeRatio"),
            ouSettings.get("publicationCategoryRatio"),
            ouSettings.get("citationCountTotal"),
            ouSettings.get("citationCountByYear"),
            ouSettings.get("viewCountTotal"),
            ouSettings.get("viewCountByMonth"),
            ouSettings.get("viewCountByCountry"),
            ouSettings.get("publicationCountPersonLeaderboard"),
            ouSettings.get("publicationCountSubUnitLeaderboard"),
            ouSettings.get("citationCountPersonLeaderboard"),
            ouSettings.get("citationCountSubUnitLeaderboard"),
            ouSettings.get("assessmentPointCountPersonLeaderboard"),
            ouSettings.get("assessmentPointCountSubUnitLeaderboard"),
            ouSettings.get("viewCountPersonLeaderboard"),
            ouSettings.get("viewCountDocumentLeaderboard"),
            ouSettings.get("downloadCountDocumentLeaderboard"),
            ouSettings.get("citationCountDocumentLeaderboard")
        ));

        var documentSettings = configuration.getDocumentChartDisplaySettings();
        response.setDocumentChartDisplaySettings(new DocumentChartDisplaySettingsDTO(
            documentSettings.get("viewCountTotal"),
            documentSettings.get("viewCountByMonth"),
            documentSettings.get("downloadCountTotal"),
            documentSettings.get("downloadCountByMonth"),
            documentSettings.get("viewCountByCountry"),
            documentSettings.get("downloadCountByCountry")
        ));

        var digitalLibrarySettings = configuration.getDigitalLibraryChartDisplaySettings();
        response.setDigitalLibraryChartDisplaySettings(new DigitalLibraryChartDisplaySettingsDTO(
            digitalLibrarySettings.get("thesisCountTotal"),
            digitalLibrarySettings.get("thesisCountByYear"),
            digitalLibrarySettings.get("thesisTypeByYear"),
            digitalLibrarySettings.get("thesisTypeRatio"),
            digitalLibrarySettings.get("thesisViewCountTotal"),
            digitalLibrarySettings.get("thesisViewCountByMonth"),
            digitalLibrarySettings.get("thesisViewCountByCountry"),
            digitalLibrarySettings.get("thesisDownloadCountTotal"),
            digitalLibrarySettings.get("thesisDownloadCountByMonth"),
            digitalLibrarySettings.get("thesisDownloadCountByCountry"),
            digitalLibrarySettings.get("viewCountThesisLeaderboard"),
            digitalLibrarySettings.get("downloadCountThesisLeaderboard")
        ));

        return response;
    }

    protected ChartsDisplayConfiguration createNewConfiguration(Integer institutionId) {
        var configuration = new ChartsDisplayConfiguration();
        configuration.setOrganisationUnit(
            organisationUnitService.findOrganisationUnitById(institutionId));

        return configuration;
    }
}
