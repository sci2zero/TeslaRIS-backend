package rs.teslaris.core.service.impl.person.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.PersonRepository;

@Component
@RequiredArgsConstructor
public class PersonEmploymentWorker {

    private final PersonRepository personRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public void savePersonEmploymentHierarchyIds(Person person, PersonIndex index) {
        person.getEmploymentInstitutionsIdHierarchy().addAll(
            index.getEmploymentInstitutionsIdHierarchy());
        personRepository.save(person);
    }
}
