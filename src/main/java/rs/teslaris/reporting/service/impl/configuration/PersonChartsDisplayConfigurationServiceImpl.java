package rs.teslaris.reporting.service.impl.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.interfaces.configuration.PersonChartsDisplayConfigurationService;

@Service
@Transactional
public class PersonChartsDisplayConfigurationServiceImpl
    extends BaseChartsDisplayConfigurationServiceImpl
    implements PersonChartsDisplayConfigurationService {

    private final InvolvementService involvementService;

    private final OrganisationUnitService organisationUnitService;


    @Autowired
    public PersonChartsDisplayConfigurationServiceImpl(
        ChartsDisplayConfigurationRepository chartsDisplayConfigurationRepository,
        InvolvementService involvementService, OrganisationUnitService organisationUnitService) {
        super(chartsDisplayConfigurationRepository);
        this.involvementService = involvementService;
        this.organisationUnitService = organisationUnitService;
    }

    @Override
    public PersonChartDisplaySettingsDTO getDisplaySettingsForPerson(Integer personId) {
        var personEmploymentInstitutions =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var configurations = new ArrayList<ChartsDisplayConfiguration>();

        personEmploymentInstitutions.forEach(employmentInstitutionId -> {
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

        var trueConfiguration = new PersonChartDisplaySettingsDTO(
            deduceBaseConfigurationFormMultipleSources(configurations,
                ChartsDisplayConfiguration::getPersonChartDisplaySettings));

        trueConfiguration.setCitationCountTotal(
            createChartSetting(configurations, "citationCountTotal",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));
        trueConfiguration.setCitationCountByYear(
            createChartSetting(configurations, "citationCountByYear",
                ChartsDisplayConfiguration::getOuChartDisplaySettings));

        return trueConfiguration;
    }

    @Override
    public void savePersonDisplaySettings(Integer institutionId,
                                          PersonChartDisplaySettingsDTO settings) {
        var existingConfiguration =
            chartsDisplayConfigurationRepository.getConfigurationForInstitution(
                institutionId);
        var configuration =
            existingConfiguration.orElseGet(ChartsDisplayConfiguration::new);

        if (Objects.isNull(configuration.getPersonChartDisplaySettings())) {
            configuration.setPersonChartDisplaySettings(new HashMap<>());
        }

        setBaseConfigurationFields(configuration.getPersonChartDisplaySettings(), settings);

        save(configuration);
    }

    private void addDefaultConfiguration(List<ChartsDisplayConfiguration> configurations) {
        var configuration = new ChartsDisplayConfiguration();

        setDefaultConfigurationValues(configuration.getPersonChartDisplaySettings());
        setPersonConfigurationSpecificDefaultFields(configuration.getPersonChartDisplaySettings());

        configurations.add(configuration);
    }
}
