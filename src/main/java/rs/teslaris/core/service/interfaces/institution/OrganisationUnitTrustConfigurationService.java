package rs.teslaris.core.service.interfaces.institution;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
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

    Page<DocumentPublicationIndex> fetchNonValidatedPublications(Integer institutionId,
                                                                 Boolean nonValidatedMetadata,
                                                                 Boolean nonValidatedFiles,
                                                                 List<DocumentPublicationType> allowedTypes,
                                                                 Pageable pageable);
}
