package rs.teslaris.importer.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.service.interfaces.CommonHarvestService;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional
public class CommonHarvestServiceImpl implements CommonHarvestService {

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitImportSourceConfigurationService
        organisationUnitImportSourceConfigurationService;


    @Override
    public boolean canPersonScanDataSources(Integer userId) {
        if (Objects.isNull(userId)) {
            return false;
        }

        var person = personService.findOne(personService.getPersonIdForUserId(userId));
        var configuration =
            organisationUnitImportSourceConfigurationService.readConfigurationForPerson(
                person.getId());

        return (configuration.importScopus() && Objects.nonNull(person.getScopusAuthorId()) &&
            !person.getScopusAuthorId().isEmpty()) ||
            (configuration.importOpenAlex() && Objects.nonNull(person.getOpenAlexId()) &&
                !person.getOpenAlexId().isEmpty()) ||
            (configuration.importWebOfScience() &&
                Objects.nonNull(person.getWebOfScienceResearcherId()) &&
                !person.getWebOfScienceResearcherId().isEmpty());
    }

    @Override
    public boolean canOUEmployeeScanDataSources(Integer organisationUnitId) {
        if (Objects.isNull(organisationUnitId)) {
            return false;
        }

        var organisationUnit = organisationUnitService.findOne(organisationUnitId);
        var configuration =
            organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
                organisationUnitId);

        return (configuration.importScopus() && !Objects.isNull(organisationUnit.getScopusAfid()) &&
            !organisationUnit.getScopusAfid().isEmpty()) ||
            (configuration.importOpenAlex() && !Objects.isNull(organisationUnit.getOpenAlexId()) &&
                !organisationUnit.getOpenAlexId().isEmpty()) ||
            configuration.importWebOfScience();
    }
}
