package rs.teslaris.project.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.project.dto.identifier.FundingIdentifierDTO;
import rs.teslaris.project.model.identifier.FundingIdentifier;

@Service
public interface FundingIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForFunding(Integer fundingId,
                                                               AccessLevel accessLevel);

    FundingIdentifier createFundingIdentifier(FundingIdentifierDTO fundingIdentifierDTO);

    void updateFundingIdentifier(Integer fundingIdentifierId,
                                 FundingIdentifierDTO fundingIdentifierDTO);
}
