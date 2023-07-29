package rs.teslaris.core.service;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.person.PersonName;

@Service
public interface PersonNameService extends JPAService<PersonName> {

    PersonName findPersonNameById(Integer personNameId);

    void deletePersonNamesWithIds(List<Integer> personNameIds);

}
