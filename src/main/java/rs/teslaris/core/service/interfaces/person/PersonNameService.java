package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PersonNameService extends JPAService<PersonName> {

    PersonName findPersonNameById(Integer personNameId);

    void deletePersonNamesWithIds(List<Integer> personNameIds);

}
