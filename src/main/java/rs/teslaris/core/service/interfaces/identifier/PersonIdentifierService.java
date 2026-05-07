package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.PersonIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PersonIdentifier;

@Service
public interface PersonIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForPerson(Integer personId,
                                                              AccessLevel accessLevel);

    PersonIdentifier createPersonIdentifier(PersonIdentifierDTO personIdentifierDTO,
                                            Integer userId);

    void updatePersonIdentifier(Integer personIdentifierId,
                                PersonIdentifierDTO personIdentifierDTO);
}
