package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.PersonFieldVisibilityDTO;
import rs.teslaris.core.model.person.PersonFieldVisibility;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PersonFieldVisibilityService extends JPAService<PersonFieldVisibility> {

    PersonFieldVisibilityDTO readPublicFieldConfiguration(Integer personId);

    void savePublicFieldConfiguration(Integer personId, PersonFieldVisibilityDTO configuration);
}
