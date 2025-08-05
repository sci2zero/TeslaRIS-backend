package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.importer.dto.OrganisationUnitImportSourceConfigurationDTO;
import rs.teslaris.importer.model.configuration.OrganisationUnitImportSourceConfiguration;

@Service
public interface OrganisationUnitImportSourceConfigurationService
    extends JPAService<OrganisationUnitImportSourceConfiguration> {

    OrganisationUnitImportSourceConfigurationDTO readConfigurationForInstitution(
        Integer institutionId);

    OrganisationUnitImportSourceConfigurationDTO readConfigurationForPerson(Integer personId);

    void saveConfigurationForInstitution(Integer institutionId,
                                         OrganisationUnitImportSourceConfigurationDTO configuration);
}
