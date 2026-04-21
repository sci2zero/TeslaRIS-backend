package rs.teslaris.core.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.repository.identifier.PersonIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PersonIdentifierJPAServiceImpl extends JPAServiceImpl<PersonIdentifier> {

    private final PersonIdentifierRepository personIdentifierRepository;


    @Autowired
    public PersonIdentifierJPAServiceImpl(PersonIdentifierRepository personIdentifierRepository) {
        this.personIdentifierRepository = personIdentifierRepository;
    }

    @Override
    protected JpaRepository<PersonIdentifier, Integer> getEntityRepository() {
        return personIdentifierRepository;
    }
}
