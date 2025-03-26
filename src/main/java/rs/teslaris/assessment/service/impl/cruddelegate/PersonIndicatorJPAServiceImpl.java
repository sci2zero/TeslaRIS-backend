package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.PersonIndicator;
import rs.teslaris.assessment.repository.PersonIndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PersonIndicatorJPAServiceImpl extends JPAServiceImpl<PersonIndicator> {

    private final PersonIndicatorRepository personIndicatorRepository;

    @Autowired
    public PersonIndicatorJPAServiceImpl(PersonIndicatorRepository personIndicatorRepository) {
        this.personIndicatorRepository = personIndicatorRepository;
    }

    @Override
    protected JpaRepository<PersonIndicator, Integer> getEntityRepository() {
        return personIndicatorRepository;
    }
}
