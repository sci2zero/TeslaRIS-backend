package rs.teslaris.core.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.DocumentIdentifier;

@Service
public interface DocumentIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForDocument(Integer documentId,
                                                                AccessLevel accessLevel);

    DocumentIdentifier createDocumentIdentifier(DocumentIdentifierDTO documentIdentifierDTO,
                                                Integer userId);

    void updateDocumentIdentifier(Integer documentIdentifierId,
                                  DocumentIdentifierDTO documentIdentifierDTO);
}
