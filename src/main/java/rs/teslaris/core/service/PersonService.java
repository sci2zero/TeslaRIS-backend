package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.Person;

@Service
public interface PersonService {

    Person findPersonById(Integer id);
}
