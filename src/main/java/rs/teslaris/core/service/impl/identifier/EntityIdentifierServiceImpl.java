package rs.teslaris.core.service.impl.identifier;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.identifier.EntityIdentifierDTO;
import rs.teslaris.core.model.identifier.EntityIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.EntityIdentifierService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;

@Service
@RequiredArgsConstructor
@Primary
@Traceable
public class EntityIdentifierServiceImpl extends JPAServiceImpl<EntityIdentifier> implements
    EntityIdentifierService {

    private final EntityIdentifierRepository entityIdentifierRepository;

    private final IdentifierService identifierService;


    @Override
    @Transactional
    public void deleteEntityIdentifier(Integer entityIdentifierId) {
        entityIdentifierRepository.delete(findOne(entityIdentifierId));
    }

    @Override
    protected JpaRepository<EntityIdentifier, Integer> getEntityRepository() {
        return entityIdentifierRepository;
    }

    protected void setCommonFields(EntityIdentifier entityIdentifier,
                                   EntityIdentifierDTO entityIdentifierDTO) {
        entityIdentifier.setValue(entityIdentifierDTO.getValue());

        entityIdentifier.setIdentifier(
            identifierService.findOne(entityIdentifierDTO.getIdentifierId()));
    }
}
