package rs.teslaris.importer.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.dto.OrganisationUnitImportSourceConfigurationDTO;
import rs.teslaris.importer.model.configuration.OrganisationUnitImportSourceConfiguration;
import rs.teslaris.importer.repository.OrganisationUnitImportSourceConfigurationRepository;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitImportSourceConfigurationServiceImpl
    extends JPAServiceImpl<OrganisationUnitImportSourceConfiguration>
    implements OrganisationUnitImportSourceConfigurationService {

    private final OrganisationUnitImportSourceConfigurationRepository
        organisationUnitImportSourceConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    @Value("${scopus.api.key}")
    private String scopusApiKey;

    @Value("${wos.api.key}")
    private String wosApiKey;


    @Override
    protected JpaRepository<OrganisationUnitImportSourceConfiguration, Integer> getEntityRepository() {
        return organisationUnitImportSourceConfigurationRepository;
    }

    @Override
    public OrganisationUnitImportSourceConfigurationDTO readConfigurationForInstitution(
        Integer institutionId) {
        var configuration =
            organisationUnitImportSourceConfigurationRepository.findConfigurationForInstitution(
                institutionId);

        var scopusConfigured = isScopusConfigured();
        var wosConfigured = isWebOfScienceConfigured();

        return configuration.map(
                organisationUnitImportSourceConfiguration -> new OrganisationUnitImportSourceConfigurationDTO(
                    organisationUnitImportSourceConfiguration
                        .getImportScopus(),
                    organisationUnitImportSourceConfiguration.getImportOpenAlex(),
                    organisationUnitImportSourceConfiguration.getImportWebOfScience(),
                    scopusConfigured, wosConfigured))
            .orElseGet(() -> new OrganisationUnitImportSourceConfigurationDTO(
                true, true, true,
                scopusConfigured, wosConfigured));
    }

    @Override
    public OrganisationUnitImportSourceConfigurationDTO readConfigurationForPerson(
        Integer personId) {
        var person = personService.findOne(personId);

        var scopusConfigured = isScopusConfigured();
        var wosConfigured = isWebOfScienceConfigured();

        var institutionIds = person.getInvolvements().stream().filter(
                involvement -> involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                    involvement.getInvolvementType().equals(InvolvementType.HIRED_BY))
            .map(Involvement::getId).toList();

        var institutionLevelConfigs = institutionIds.stream().map(
            institutionId -> organisationUnitImportSourceConfigurationRepository.findConfigurationForInstitution(
                institutionId).orElse(null)).filter(Objects::nonNull).toList();

        if (institutionLevelConfigs.isEmpty()) {
            return new OrganisationUnitImportSourceConfigurationDTO(
                scopusConfigured, true, wosConfigured,
                scopusConfigured, wosConfigured);
        }

        return new OrganisationUnitImportSourceConfigurationDTO(
            institutionLevelConfigs.stream().anyMatch(
                OrganisationUnitImportSourceConfiguration::getImportScopus) && scopusConfigured,
            institutionLevelConfigs.stream().anyMatch(
                OrganisationUnitImportSourceConfiguration::getImportOpenAlex),
            institutionLevelConfigs.stream().anyMatch(
                OrganisationUnitImportSourceConfiguration::getImportWebOfScience) && wosConfigured,
            scopusConfigured, wosConfigured);
    }

    @Override
    public void saveConfigurationForInstitution(Integer institutionId,
                                                OrganisationUnitImportSourceConfigurationDTO configuration) {
        organisationUnitImportSourceConfigurationRepository.findConfigurationForInstitution(
            institutionId).ifPresentOrElse((existingConfiguration) -> {
            existingConfiguration.setImportScopus(configuration.importScopus());
            existingConfiguration.setImportOpenAlex(configuration.importOpenAlex());
            existingConfiguration.setImportWebOfScience(configuration.importWebOfScience());
            save(existingConfiguration);
        }, () -> {
            var institution = organisationUnitService.findOne(institutionId);
            var newConfiguration = new OrganisationUnitImportSourceConfiguration();
            newConfiguration.setImportScopus(configuration.importScopus());
            newConfiguration.setImportOpenAlex(configuration.importOpenAlex());
            newConfiguration.setImportWebOfScience(configuration.importWebOfScience());
            newConfiguration.setOrganisationUnit(institution);
            save(newConfiguration);
        });
    }

    private boolean isScopusConfigured() {
        return Objects.nonNull(scopusApiKey) && !scopusApiKey.isBlank();
    }

    private boolean isWebOfScienceConfigured() {
        return Objects.nonNull(wosApiKey) && !wosApiKey.isBlank();
    }
}
