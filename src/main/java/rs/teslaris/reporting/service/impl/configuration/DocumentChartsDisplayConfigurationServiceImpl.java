package rs.teslaris.reporting.service.impl.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.DocumentChartsDisplayConfigurationService;

@Service
@Transactional
public class DocumentChartsDisplayConfigurationServiceImpl
    extends BaseChartsDisplayConfigurationServiceImpl implements
    DocumentChartsDisplayConfigurationService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final OrganisationUnitService organisationUnitService;


    @Autowired
    public DocumentChartsDisplayConfigurationServiceImpl(
        ChartsDisplayConfigurationRepository chartsDisplayConfigurationRepository,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        OrganisationUnitService organisationUnitService) {
        super(chartsDisplayConfigurationRepository);
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.organisationUnitService = organisationUnitService;
    }

    @Override
    public DocumentChartDisplaySettingsDTO getDisplaySettingsForDocument(Integer documentId) {
        var documentIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (documentIndex.isEmpty()) {
            throw new NotFoundException("Document with ID " + documentId + " does not exist.");
        }

        var institutionIds = documentIndex.get().getOrganisationUnitIdsActive();
        var configurations = new ArrayList<ChartsDisplayConfiguration>();

        institutionIds.forEach(employmentInstitutionId -> {
            var currentInstitutionId = employmentInstitutionId;

            while (Objects.nonNull(currentInstitutionId)) {
                var configuration =
                    chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                        currentInstitutionId);

                if (configuration.isPresent()) {
                    configurations.add(configuration.get());
                    break;
                }

                var superRelation =
                    organisationUnitService.getSuperOrganisationUnitRelation(currentInstitutionId);
                currentInstitutionId =
                    Objects.nonNull(superRelation) ?
                        superRelation.getTargetOrganisationUnit().getId() :
                        null;
            }
        });

        if (configurations.isEmpty()) {
            addDefaultConfiguration(configurations);
        }

        var trueConfiguration = deduceDocumentConfigurationFormMultipleSources(configurations,
            ChartsDisplayConfiguration::getDocumentChartDisplaySettings);

        return new DocumentChartDisplaySettingsDTO(
            trueConfiguration.viewCountTotal(),
            trueConfiguration.viewCountByMonth(),
            trueConfiguration.downloadCountTotal(),
            trueConfiguration.downloadCountByMonth(),
            trueConfiguration.viewCountByCountry(),
            trueConfiguration.downloadCountByCountry()
        );
    }

    @Override
    public void saveDocumentDisplaySettings(Integer institutionId,
                                            DocumentChartDisplaySettingsDTO settings) {
        var existingConfiguration =
            chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                institutionId);
        var configuration =
            existingConfiguration.orElseGet(ChartsDisplayConfiguration::new);

        if (Objects.isNull(configuration.getDocumentChartDisplaySettings())) {
            configuration.setDocumentChartDisplaySettings(new HashMap<>());
        }

        configuration.getDocumentChartDisplaySettings()
            .put("viewCountTotal", settings.viewCountTotal());
        configuration.getDocumentChartDisplaySettings()
            .put("viewCountByMonth", settings.viewCountByMonth());
        configuration.getDocumentChartDisplaySettings()
            .put("downloadCountTotal", settings.downloadCountTotal());
        configuration.getDocumentChartDisplaySettings()
            .put("downloadCountByMonth", settings.downloadCountByMonth());
        configuration.getDocumentChartDisplaySettings()
            .put("viewCountByCountry", settings.viewCountByCountry());
        configuration.getDocumentChartDisplaySettings()
            .put("downloadCountByCountry", settings.downloadCountByCountry());

        save(configuration);
    }

    private void addDefaultConfiguration(List<ChartsDisplayConfiguration> configurations) {
        var configuration = new ChartsDisplayConfiguration();

        setDocumentConfigurationSpecificDefaultFields(
            configuration.getDocumentChartDisplaySettings());

        configurations.add(configuration);
    }

    protected DocumentChartDisplaySettingsDTO deduceDocumentConfigurationFormMultipleSources(
        List<ChartsDisplayConfiguration> configurations,
        Function<ChartsDisplayConfiguration, Map<String, ChartDisplaySettings>> settingsExtractor) {
        return new DocumentChartDisplaySettingsDTO(
            createChartSetting(configurations, "viewCountTotal", settingsExtractor),
            createChartSetting(configurations, "viewCountByMonth", settingsExtractor),
            createChartSetting(configurations, "downloadCountTotal", settingsExtractor),
            createChartSetting(configurations, "downloadCountByMonth", settingsExtractor),
            createChartSetting(configurations, "viewCountByCountry", settingsExtractor),
            createChartSetting(configurations, "downloadCountByCountry", settingsExtractor)
        );
    }
}
