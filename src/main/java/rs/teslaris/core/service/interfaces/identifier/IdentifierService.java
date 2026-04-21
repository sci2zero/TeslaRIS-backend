package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.IdentifierDTO;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface IdentifierService extends JPAService<Identifier> {

    Page<IdentifierResponseDTO> readAllIdentifiers(Pageable pageable, String language);

    List<IdentifierResponseDTO> getIdentifiersApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    IdentifierResponseDTO readIdentifierById(Integer identifierId);

    AccessLevel readIdentifierAccessLevel(Integer identifierId);

    Identifier getIdentifierByCode(String code);

    Identifier createIdentifier(IdentifierDTO identifierDTO);

    void updateIdentifier(Integer identifierId, IdentifierDTO identifierDTO);

    void deleteIdentifier(Integer identifierId);
}
