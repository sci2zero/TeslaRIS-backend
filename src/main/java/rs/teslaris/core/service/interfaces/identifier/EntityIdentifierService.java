package rs.teslaris.core.service.interfaces.identifier;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.identifier.EntityIdentifier;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface EntityIdentifierService extends JPAService<EntityIdentifier> {

    void deleteEntityIdentifier(Integer entityIdentifierId);
}
