package rs.teslaris.core.service.impl;

import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.person.PersonNameRepository;
import rs.teslaris.core.service.PersonNameService;

@Service
@RequiredArgsConstructor
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
