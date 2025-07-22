package rs.teslaris.core.service.interfaces.institution;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.model.institution.OrganisationUnitTrustConfiguration;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.Pair;

@Service
public interface OrganisationUnitTrustConfigurationService
    extends JPAService<OrganisationUnitTrustConfiguration> {

    OrganisationUnitTrustConfigurationDTO readTrustConfigurationForOrganisationUnit(
        Integer organisationUnitId);

    OrganisationUnitTrustConfigurationDTO saveConfiguration(
        OrganisationUnitTrustConfigurationDTO dto, Integer organisationUnitId);

    void approvePublicationMetadata(Integer documentId);

    void approvePublicationUploadedDocuments(Integer documentId);

    Pair<Boolean, Boolean> fetchValidationStatusForDocument(Integer documentId);
}
