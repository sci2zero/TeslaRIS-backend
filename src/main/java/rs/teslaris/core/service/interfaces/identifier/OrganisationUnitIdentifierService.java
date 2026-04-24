package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.OrganisationUnitIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;

@Service
public interface OrganisationUnitIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForOrganisationUnit(Integer organisationUnitId,
                                                                        AccessLevel accessLevel);

    OrganisationUnitIdentifier createOrganisationUnitIdentifier(
        OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO, Integer userId);

    void updateOrganisationUnitIdentifier(Integer organisationUnitIdentifierId,
                                          OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO);
}
