package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.PublicationSeriesIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PublicationSeriesIdentifier;

@Service
public interface PublicationSeriesIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForPublicationSeries(
        Integer publicationSeriesId,
        AccessLevel accessLevel);

    PublicationSeriesIdentifier createPublicationSeriesIdentifier(
        PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO, Integer userId);

    void updatePublicationSeriesIdentifier(Integer publicationSeriesIdentifierId,
                                           PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO);
}
