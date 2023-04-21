package rs.teslaris.core.service;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.model.person.Person;

@Service
public interface PersonService {

    Person findPersonById(Integer id);

    Person createPersonWithBasicInfo(BasicPersonDTO personDTO);

    void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId);

    void updatePersonalInfo(PersonalInfoDTO personalInfo, Integer personId);
}
