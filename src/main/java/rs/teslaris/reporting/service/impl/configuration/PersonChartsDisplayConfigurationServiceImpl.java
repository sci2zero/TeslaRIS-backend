package rs.teslaris.reporting.service.impl.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.reporting.PersonChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.PersonChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.PersonChartsDisplayConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonChartsDisplayConfigurationServiceImpl
    extends JPAServiceImpl<PersonChartsDisplayConfiguration>
    implements PersonChartsDisplayConfigurationService {

    private final PersonChartsDisplayConfigurationRepository
        personChartsDisplayConfigurationRepository;

    private final InvolvementService involvementService;

    private final OrganisationUnitService organisationUnitService;


    @Override
    public PersonChartDisplaySettingsDTO getDisplaySettingsForPerson(Integer personId) {
        var personEmploymentInstitutions =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var configurations = new ArrayList<PersonChartsDisplayConfiguration>();

        personEmploymentInstitutions.forEach(employmentInstitutionId -> {
            var currentInstitutionId = employmentInstitutionId;

            while (Objects.nonNull(currentInstitutionId)) {
                var configuration =
                    personChartsDisplayConfigurationRepository.getConfigurationForInstitution(
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

        return new PersonChartDisplaySettingsDTO(
            createChartSetting(configurations, "publicationCountTotal"),
            createChartSetting(configurations, "publicationCountByYear"),
            createChartSetting(configurations, "publicationTypeByYear"),
            createChartSetting(configurations, "publicationCategoryByYear"),
            createChartSetting(configurations, "publicationTypeRatio"),
            createChartSetting(configurations, "publicationCategoryRatio"),
            createChartSetting(configurations, "citationCountTotal"),
            createChartSetting(configurations, "citationCountByYear"),
            createChartSetting(configurations, "viewCountTotal"),
            createChartSetting(configurations, "viewCountByMonth"),
            createChartSetting(configurations, "viewCountByCountry")
        );
    }

    @Override
    public void savePersonDisplaySettings(Integer institutionId,
                                          PersonChartDisplaySettingsDTO settings) {
        var existingConfiguration =
            personChartsDisplayConfigurationRepository.getConfigurationForInstitution(
                institutionId);
        var configuration =
            existingConfiguration.orElseGet(PersonChartsDisplayConfiguration::new);

        if (Objects.isNull(configuration.getChartDisplaySettings())) {
            configuration.setChartDisplaySettings(new HashMap<>());
        }

        configuration.getChartDisplaySettings()
            .put("publicationCountTotal", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("publicationCountByYear", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("publicationTypeByYear", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("publicationCategoryByYear", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("publicationTypeRatio", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("publicationCategoryRatio", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("citationCountTotal", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("citationCountByYear", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("viewCountTotal", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("viewCountByMonth", settings.publicationCountTotal());
        configuration.getChartDisplaySettings()
            .put("viewCountByCountry", settings.publicationCountTotal());

        save(configuration);
    }

    @Override
    protected JpaRepository<PersonChartsDisplayConfiguration, Integer> getEntityRepository() {
        return personChartsDisplayConfigurationRepository;
    }

    private ChartDisplaySettings createChartSetting(
        List<PersonChartsDisplayConfiguration> configurations, String chartKey) {
        boolean display = configurations.stream()
            .anyMatch(conf -> conf.getChartDisplaySettings()
                .getOrDefault(chartKey, new ChartDisplaySettings(true, true)).getDisplay());
        boolean spanWholeRow = configurations.stream()
            .anyMatch(conf -> conf.getChartDisplaySettings()
                .getOrDefault(chartKey, new ChartDisplaySettings(true, true)).getSpanWholeRow());
        return new ChartDisplaySettings(display, spanWholeRow);
    }

    private void addDefaultConfiguration(List<PersonChartsDisplayConfiguration> configurations) {
        var configuration = new PersonChartsDisplayConfiguration();

        configuration.getChartDisplaySettings()
            .put("publicationCountTotal", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("publicationCountByYear", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("publicationTypeByYear", new ChartDisplaySettings(true, true));
        configuration.getChartDisplaySettings()
            .put("publicationCategoryByYear", new ChartDisplaySettings(true, true));
        configuration.getChartDisplaySettings()
            .put("publicationTypeRatio", new ChartDisplaySettings(true, true));
        configuration.getChartDisplaySettings()
            .put("publicationCategoryRatio", new ChartDisplaySettings(true, true));
        configuration.getChartDisplaySettings()
            .put("citationCountTotal", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("citationCountByYear", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("viewCountTotal", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("viewCountByMonth", new ChartDisplaySettings(true, false));
        configuration.getChartDisplaySettings()
            .put("viewCountByCountry", new ChartDisplaySettings(true, true));

        configurations.add(configuration);
    }
}
