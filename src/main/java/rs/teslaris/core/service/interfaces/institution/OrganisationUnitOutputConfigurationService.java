package rs.teslaris.core.service.interfaces.institution;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.model.institution.OrganisationUnitOutputConfiguration;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface OrganisationUnitOutputConfigurationService
    extends JPAService<OrganisationUnitOutputConfiguration> {

    OrganisationUnitOutputConfigurationDTO readOutputConfigurationForOrganisationUnit(
        Integer organisationUnitId);

    OrganisationUnitOutputConfigurationDTO saveConfiguration(
        OrganisationUnitOutputConfigurationDTO dto, Integer organisationUnitId);
}
