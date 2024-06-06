package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonUserResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PersonService extends JPAService<Person> {

    Page<PersonIndex> findAllIndex(Pageable pageable);

    Long getResearcherCount();

    Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable);

    Page<PersonIndex> findPeopleForOrganisationUnit(Integer employmentInstitutionId,
                                                    Pageable pageable);

    Page<PersonIndex> advancedSearch(List<String> tokens, Pageable pageable);

    Person findPersonById(Integer id);

    PersonResponseDTO readPersonByScopusId(String scopusAuthorId);

    Optional<User> findUserByScopusAuthorId(String scopusAuthorId);

    Person findPersonByOldId(Integer id);

    PersonResponseDTO readPersonWithBasicInfo(Integer id);

    PersonUserResponseDTO readPersonWithUser(Integer id);

    boolean isPersonEmployedInOrganisationUnit(Integer personId, Integer organisationUnitId);

    Person createPersonWithBasicInfo(BasicPersonDTO personDTO, Boolean index);

    void setPersonBiography(List<MultilingualContentDTO> biography, Integer personId);

    void setPersonKeyword(List<MultilingualContentDTO> keyword, Integer personId);

    void setPersonMainName(Integer personNameId, Integer personId);

    void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId);

    void updatePersonalInfo(PersonalInfoDTO personalInfo, Integer personId);

    void approvePerson(Integer personId, Boolean approved);

    void deletePerson(Integer personId);

    Involvement getLatestResearcherInvolvement(Person person);

    InvolvementDTO getLatestResearcherInvolvement(Integer personId);

    void reindexPersons();

    void indexPerson(Person savedPerson, Integer personDatabaseId);


}
