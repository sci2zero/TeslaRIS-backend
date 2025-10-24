package rs.teslaris.core.service.interfaces.person;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.ImportPersonDTO;
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
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;

@Service
public interface PersonService extends JPAService<Person> {

    Page<PersonIndex> findAllIndex(Pageable pageable);

    Long getResearcherCount();

    Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable,
                                                    boolean strict, Integer institutionId,
                                                    boolean onlyHarvestable);

    Page<PersonIndex> findPeopleForOrganisationUnit(Integer employmentInstitutionId,
                                                    List<String> tokens,
                                                    Pageable pageable, Boolean fetchAlumni);

    Page<PersonIndex> advancedSearch(List<String> tokens, Pageable pageable);

    PersonIndex findPersonByImportIdentifier(String identifier);

    Person findPersonById(Integer id);

    PersonResponseDTO readPersonByScopusId(String scopusAuthorId);

    Optional<User> findUserByIdentifier(String identifier);

    Person findPersonByOldId(Integer id);

    PersonResponseDTO readPersonWithBasicInfo(Integer id);

    PersonResponseDTO readPersonWithBasicInfoForOldId(Integer oldId);

    PersonUserResponseDTO readPersonWithUser(Integer id);

    boolean isPersonEmployedInOrganisationUnit(Integer personId, Integer organisationUnitId);

    Person createPersonWithBasicInfo(BasicPersonDTO personDTO, Boolean index);

    Person importPersonWithBasicInfo(ImportPersonDTO personDTO, Boolean index);

    void setPersonBiography(List<MultilingualContentDTO> biography, Integer personId);

    void setPersonKeyword(List<MultilingualContentDTO> keyword, Integer personId);

    void setPersonMainName(Integer personNameId, Integer personId);

    void updatePersonMainName(Integer personId, PersonNameDTO personNameDTO);

    void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId);

    void addPersonOtherName(PersonNameDTO personNameDTO, Integer personId);

    void updatePersonalInfo(Integer personId, PersonalInfoDTO personalInfo);

    void approvePerson(Integer personId, Boolean approved);

    void deletePerson(Integer personId);

    void forceDeletePerson(Integer personId);

    Involvement getLatestResearcherInvolvement(Person person);

    InvolvementDTO getLatestResearcherInvolvement(Integer personId);

    CompletableFuture<Void> reindexPersons();

    void indexPerson(Person savedPerson);

    Integer getPersonIdForUserId(Integer userId);

    List<Integer> findInstitutionIdsForPerson(Integer personId);

    boolean isPersonBoundToAUser(Integer personId);

    void switchToUnmanagedEntity(Integer personId);

    String setPersonProfileImage(Integer personId, ProfilePhotoOrLogoDTO profilePhotoDTO)
        throws IOException;

    void removePersonProfileImage(Integer personId);

    boolean isIdentifierInUse(String identifier, Integer personId);

    List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields);

    Person findPersonByAccountingId(String accountingId);

    Page<Person> findPersonsByLRUHarvest(Pageable pageable);

    Person findRaw(Integer personId);

    void addOldId(Integer id, Integer oldId);

    Optional<Person> findPersonByIdentifier(String identifier);

    List<Pair<String, Integer>> getTopCoauthorsForPerson(Integer personId);

    String getPersonProfileImageServerFilename(Integer personId);
}
