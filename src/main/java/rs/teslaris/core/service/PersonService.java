package rs.teslaris.core.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDto;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.model.person.Person;

@Service
public interface PersonService {

    Page<PersonIndex> findAll(Pageable pageable);

    Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable);

    Page<PersonIndex> findPeopleForOrganisationUnit(Integer employmentInstitutionId,
                                                    Pageable pageable);

    Person findPersonById(Integer id);

    PersonResponseDto readPersonWithBasicInfo(Integer id);

    boolean isPersonEmployedInOrganisationUnit(Integer personId, Integer organisationUnitId);

    Person createPersonWithBasicInfo(BasicPersonDTO personDTO);

    void setPersonBiography(List<MultilingualContentDTO> biography, Integer personId);

    void setPersonKeyword(List<MultilingualContentDTO> keyword, Integer personId);

    void setPersonMainName(Integer personNameId, Integer personId);

    void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId);

    void updatePersonalInfo(PersonalInfoDTO personalInfo, Integer personId);

    void approvePerson(Integer personId, Boolean approved);
}
