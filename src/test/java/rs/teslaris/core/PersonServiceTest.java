package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.converter.person.PersonConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDto;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.CountryService;
import rs.teslaris.core.service.LanguageTagService;
import rs.teslaris.core.service.PersonNameService;
import rs.teslaris.core.service.impl.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.PersonServiceImpl;

@SpringBootTest
public class PersonServiceTest {

    @Mock
    private OrganisationUnitServiceImpl organisationUnitService;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private PersonNameService personNameService;

    @Mock
    private CountryService countryService;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @Mock
    private ElasticsearchOperations template;

    @InjectMocks
    private PersonServiceImpl personService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(personService, "approvedByDefault", true);
    }

    @Test
    public void shouldReturnPersonWhenPersonExists() {
        // given
        var expectedPerson = new Person();

        when(personRepository.findById(1)).thenReturn(Optional.of(expectedPerson));

        // when
        Person actualPerson = personService.findOne(1);

        // then
        assertEquals(expectedPerson, actualPerson);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonDoesNotExist() {
        // given
        when(personRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> personService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReadPersonWhenPersonIsApproved() {
        // given
        var expectedPerson = new Person();
        var expectedResponse = new PersonResponseDto();

        when(personRepository.findApprovedPersonById(1)).thenReturn(Optional.of(expectedPerson));

        MockedStatic<PersonConverter> mocked = mockStatic(PersonConverter.class);
        mocked.when(() -> PersonConverter.toDTO(expectedPerson)).thenReturn(expectedResponse);

        // when
        var personDto = personService.readPersonWithBasicInfo(1);

        // then
        assertEquals(personDto, expectedResponse);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonIsNotApproved() {
        // given
        when(personRepository.findApprovedPersonById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> personService.readPersonWithBasicInfo(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    void shouldCreatePersonWithBasicInfoWhenInfoIsValid() {
        // given
        var personDTO = new BasicPersonDTO();
        var personNameDTO = new PersonNameDTO();
        personNameDTO.setFirstname("John");
        personNameDTO.setLastname("Doe");
        personDTO.setPersonName(personNameDTO);
        personDTO.setContactEmail("john.doe@example.com");
        personDTO.setSex(Sex.MALE);
        personDTO.setLocalBirthDate(LocalDate.of(1985, 5, 15));
        personDTO.setPhoneNumber("+1-555-555-5555");
        personDTO.setApvnt("12345");
        personDTO.setMnid("67890");
        personDTO.setOrcid("0000-0002-1825-0097");
        personDTO.setScopusAuthorId("00000000000");
        personDTO.setOrganisationUnitId(1);
        personDTO.setEmploymentPosition(EmploymentPosition.ASSISTANT_PROFESSOR);

        var personalInfo = new PersonalInfo();
        personalInfo.setLocalBirthDate(LocalDate.now());

        var person = new Person();
        person.setName(new PersonName());
        person.setInvolvements(new HashSet<>());
        person.setPersonalInfo(personalInfo);
        person.setApproveStatus(ApproveStatus.APPROVED);

        // when
        var employmentInstitution = new OrganisationUnit();
        when(organisationUnitService.findOrganisationUnitById(2)).thenReturn(
            employmentInstitution);
        when(personRepository.save(any(Person.class))).thenReturn(person);

        // then
        var result = personService.createPersonWithBasicInfo(personDTO);
        assertNotNull(result);
        assertEquals("John", result.getName().getFirstname());
        assertEquals("Doe", result.getName().getLastname());
        assertEquals("john.doe@example.com",
            result.getPersonalInfo().getContact().getContactEmail());
        assertEquals(Sex.MALE, result.getPersonalInfo().getSex());
        assertEquals(LocalDate.of(1985, 5, 15), result.getPersonalInfo().getLocalBirthDate());
        assertEquals("+1-555-555-5555", result.getPersonalInfo().getContact().getPhoneNumber());
        assertEquals("12345", result.getApvnt());
        assertEquals("67890", result.getMnid());
        assertEquals("0000-0002-1825-0097", result.getOrcid());
        assertEquals("00000000000", result.getScopusAuthorId());
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        assertEquals(1, result.getInvolvements().size());
        var currentEmployment = result.getInvolvements().iterator().next();
        assertEquals(ApproveStatus.APPROVED, currentEmployment.getApproveStatus());
        assertEquals(InvolvementType.EMPLOYED_AT, currentEmployment.getInvolvementType());
        assertEquals(EmploymentPosition.ASSISTANT_PROFESSOR,
            ((Employment) currentEmployment).getEmploymentPosition());
    }

    @Test
    public void shouldSetPersonBiographyWithAnyData() {
        // given
        var person = new Person();
        person.setBiography(new HashSet<>());
        var bio1 = new MultilingualContentDTO(1, "English content", 1);
        var bio2 = new MultilingualContentDTO(2, "Contenu français", 2);
        var bioList = Arrays.asList(bio1, bio2);

        when(personRepository.findById(1)).thenReturn(Optional.of(person));
        when(languageTagService.findOne(anyInt())).thenReturn(
            new LanguageTag("en", "English"));

        // when
        personService.setPersonBiography(bioList, 1);

        // then
        verify(personRepository, times(2)).save(any(Person.class));
    }

    @Test
    public void shouldSetPersonKeywordWithAnyData() {
        // given
        var person = new Person();
        person.setBiography(new HashSet<>());
        var keyword1 = new MultilingualContentDTO(1, "English content", 1);
        var keyword2 = new MultilingualContentDTO(2, "Contenu français", 2);
        var keywordList = Arrays.asList(keyword1, keyword2);

        when(personRepository.findById(1)).thenReturn(Optional.of(person));
        when(languageTagService.findOne(anyInt())).thenReturn(
            new LanguageTag("en", "English"));

        // when
        personService.setPersonBiography(keywordList, 1);

        // then
        verify(personRepository, times(2)).save(any(Person.class));
    }

    @Test
    public void shouldSetPersonMainName() {
        // given
        var personName1 = new PersonName("Stan", "John", "Doe", null, null);
        var personName2 = new PersonName("Stan", "Jonny", "Doe", null, null);

        var personalInfo = new PersonalInfo();
        personalInfo.setLocalBirthDate(LocalDate.now());

        var person = new Person();
        person.setId(1);
        person.setOtherNames(new HashSet<>());
        person.getOtherNames().add(personName2);
        person.setName(personName1);
        person.setPersonalInfo(personalInfo);
        person.setInvolvements(new HashSet<>());
        person.setApproveStatus(ApproveStatus.APPROVED);

        when(personRepository.findById(1)).thenReturn(Optional.of(person));
        when(personNameService.findOne(2)).thenReturn(personName2);
        when(personIndexRepository.findByDatabaseId(anyInt())).thenReturn(
            Optional.of(new PersonIndex()));

        // when
        personService.setPersonMainName(2, 1);

        // then
        verify(personRepository, times(1)).save(person);
        assertEquals(person.getName().getOtherName(), personName2.getOtherName());
    }

    @Test
    void setPersonOtherNames_shouldSetOtherNamesForPerson() {
        // given
        var personId = 1;
        var personNameDTOList = new ArrayList<PersonNameDTO>();
        personNameDTOList.add(new PersonNameDTO("John", "Doe", "Smith", null, null));
        personNameDTOList.add(new PersonNameDTO("Jane", "Marie", "Doe", null, null));

        var personToUpdate = new Person();
        personToUpdate.setId(personId);
        personToUpdate.setOtherNames(new HashSet<>());

        when(personRepository.findById(personId)).thenReturn(Optional.of(personToUpdate));

        // when
        personService.setPersonOtherNames(personNameDTOList, personId);

        // then
        verify(personRepository, times(1)).findById(personId);
        verify(personRepository, times(2)).save(personToUpdate);
    }

    @Test
    void setPersonOtherNames_shouldDeleteExistingOtherNames() {
        // given
        var personId = 1;
        var personNameDTOList = new ArrayList<PersonNameDTO>();
        personNameDTOList.add(new PersonNameDTO("John", "Doe", "Smith", null, null));

        var personToUpdate = new Person();
        personToUpdate.setId(personId);

        var personNames = new HashSet<PersonName>();
        personNames.add(new PersonName("Jane", "Marie", "Doe", null, null));
        personToUpdate.setOtherNames(personNames);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personToUpdate));

        // when
        personService.setPersonOtherNames(personNameDTOList, personId);

        // then
        verify(personNameService, times(1)).deletePersonNamesWithIds(anyList());
        assertEquals(1, personToUpdate.getOtherNames().size());
    }

    @Test
    void setPersonOtherNames_shouldThrowExceptionIfPersonNotFound() {
        // given
        var personId = 1;
        var personNameDTOList = new ArrayList<PersonNameDTO>();
        personNameDTOList.add(new PersonNameDTO("John", "Doe", "Smith", null, null));

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> {
            personService.setPersonOtherNames(personNameDTOList, personId);
        });

        // then (NotFoundException should be thrown)
    }

    @Test
    void updatePersonalInfo_shouldUpdatePersonalInfoForPerson() {
        // given
        var personId = 1;
        var personalInfoDTO = new PersonalInfoDTO();
        personalInfoDTO.setPlaceOfBrith("City");
        personalInfoDTO.setApvnt("Mr.");
        personalInfoDTO.setMnid("123456");
        personalInfoDTO.setOrcid("0000-0000-0000-0000");
        personalInfoDTO.setScopusAuthorId("1234567");
        personalInfoDTO.setPostalAddress(
            new PostalAddressDTO(1, new ArrayList<>(), new ArrayList<>()));
        personalInfoDTO.setContact(new ContactDTO("email", "phone"));
        personalInfoDTO.setLocalBirthDate(LocalDate.of(1990, 1, 1));
        personalInfoDTO.setSex(Sex.MALE);

        var personalInfo = new PersonalInfo();

        var postalAddress = new PostalAddress();
        postalAddress.setCountry(new Country());
        postalAddress.setCity(new HashSet<>());
        postalAddress.setStreetAndNumber(new HashSet<>());
        personalInfo.setPostalAddress(postalAddress);

        var contact = new Contact();
        personalInfo.setContact(contact);

        var personToUpdate = new Person();
        personToUpdate.setId(personId);
        personToUpdate.setPersonalInfo(personalInfo);
        personToUpdate.setName(new PersonName());
        personToUpdate.setInvolvements(new HashSet<>());
        personToUpdate.setApproveStatus(ApproveStatus.APPROVED);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personToUpdate));
        when(countryService.findOne(anyInt())).thenReturn(new Country());
        when(personRepository.save(any(Person.class))).thenReturn(personToUpdate);
        when(personIndexRepository.findByDatabaseId(anyInt())).thenReturn(
            Optional.of(new PersonIndex()));

        // when
        personService.updatePersonalInfo(personalInfoDTO, personId);

        // then
        verify(personRepository, times(1)).findById(personId);
        verify(personRepository, times(1)).save(personToUpdate);
        assertEquals("Mr.", personToUpdate.getApvnt());
        assertEquals("123456", personToUpdate.getMnid());
        assertEquals("0000-0000-0000-0000", personToUpdate.getOrcid());
        assertEquals("1234567", personToUpdate.getScopusAuthorId());
        assertEquals("City", personalInfo.getPlaceOfBrith());
        assertEquals(LocalDate.of(1990, 1, 1), personalInfo.getLocalBirthDate());
        assertEquals(Sex.MALE, personalInfo.getSex());
        assertNotNull(personalInfo.getPostalAddress().getCountry());
        assertEquals(0, personalInfo.getPostalAddress().getStreetAndNumber().size());
        assertEquals(0, personalInfo.getPostalAddress().getCity().size());
        assertEquals("email", personalInfo.getContact().getContactEmail());
        assertEquals("phone", personalInfo.getContact().getPhoneNumber());
    }

    @Test
    void updatePersonalInfo_shouldThrowExceptionIfPersonNotFound() {
        // given
        var personId = 1;
        var personalInfoDTO = new PersonalInfoDTO();

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> {
            personService.updatePersonalInfo(personalInfoDTO, personId);
        });

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldFindAll() {
        // Given
        var pageable = Pageable.ofSize(10).withPage(0);
        var expected = new PageImpl<>(List.of(new PersonIndex(), new PersonIndex()));

        when(personIndexRepository.findAll(pageable)).thenReturn(expected);

        // When
        var actual = personService.findAllIndex(pageable);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    void shouldFindPeopleByNameAndEmploymentWhenProperQueryIsGiven() throws Exception {
        // given
        var tokens = Arrays.asList("Ivan", "FTN");
        var pageable = PageRequest.of(0, 10);

        var searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(2L);

        when(template.search((Query) any(), any(), any())).thenReturn(searchHits);

        // when
        var result = personService.findPeopleByNameAndEmployment(tokens, pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    void shouldFindPeopleForOrganisationUnitWhenGivenValidId() {
        // given
        var employmentInstitutionId = 123;
        var pageable = PageRequest.of(0, 10);

        when(personIndexRepository.findByEmploymentInstitutionsIdIn(pageable,
            List.of(employmentInstitutionId))).thenReturn(
            new PageImpl<>(List.of(new PersonIndex())));

        // when
        var result =
            personService.findPeopleForOrganisationUnit(employmentInstitutionId, pageable);

        // then
        assertEquals(result.getTotalElements(), 1L);
    }

}
