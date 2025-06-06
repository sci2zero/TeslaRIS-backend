package rs.teslaris.core.service.impl.person;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.person.PersonNameRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonNameService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Traceable
public class PersonNameServiceImpl extends JPAServiceImpl<PersonName> implements PersonNameService {

    private final PersonNameRepository personNameRepository;

    @Override
    protected JpaRepository<PersonName, Integer> getEntityRepository() {
        return personNameRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public PersonName findPersonNameById(Integer personNameId) {
        return personNameRepository.findById(personNameId)
            .orElseThrow(() -> new NotFoundException("Person name with given ID does not exist."));
    }

    @Override
    @Transactional
    public void deletePersonNamesWithIds(List<Integer> personNameIds) {
        for (var id : personNameIds) {
            this.delete(id);
        }
    }
}
