package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.converter.person.PersonConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
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
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.impl.person.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.person.PersonServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.PersonNameService;
import rs.teslaris.core.util.ImageUtil;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;

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
    private SearchService<PersonIndex> searchService;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private PersonContributionRepository personContributionRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private FileService fileService;

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
    public void shouldReturnPersonByOldIdWhenPersonExists() {
        // given
        var expectedPerson = new Person();
        expectedPerson.setId(10);
        expectedPerson.setName(new PersonName());
        var personalInfo = new PersonalInfo();
        personalInfo.setPostalAddress(new PostalAddress());
        expectedPerson.setPersonalInfo(personalInfo);

        when(personRepository.findPersonByOldIdsContains(1)).thenReturn(
            Optional.of(expectedPerson));

        // when
        var actualPerson = personService.readPersonWithBasicInfoForOldId(1);

        // then
        verify(personRepository, times(1)).findPersonByOldIdsContains(1);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonDoesNotExistWithOldId() {
        // given
        when(personRepository.findPersonByOldIdsContains(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> personService.readPersonWithBasicInfoForOldId(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReadPersonWhenPersonIsApproved() {
        // given
        var expectedPerson = new Person();
        var expectedResponse = new PersonResponseDTO();

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
        personDTO.setECrisId("67890");
        personDTO.setENaukaId("rp67890");
        personDTO.setOrcid("0000-0002-1825-0097");
        personDTO.setScopusAuthorId("00000000000");
        personDTO.setOrganisationUnitId(1);
        personDTO.setEmploymentPosition(EmploymentPosition.ASSISTANT_PROFESSOR);

        var personalInfo = new PersonalInfo();
        personalInfo.setLocalBirthDate(LocalDate.now());

        var person = new Person();
        person.setName(new PersonName());
        person.setPersonalInfo(personalInfo);
        person.setApproveStatus(ApproveStatus.APPROVED);

        // when
        var employmentInstitution = new OrganisationUnit();
        when(organisationUnitService.findOrganisationUnitById(2)).thenReturn(
            employmentInstitution);
        when(personRepository.save(any(Person.class))).thenReturn(person);

        // then
        var result = personService.createPersonWithBasicInfo(personDTO, true);
        assertNotNull(result);
        assertEquals("John", result.getName().getFirstname());
        assertEquals("Doe", result.getName().getLastname());
        assertEquals("john.doe@example.com",
            result.getPersonalInfo().getContact().getContactEmail());
        assertEquals(Sex.MALE, result.getPersonalInfo().getSex());
        assertEquals(LocalDate.of(1985, 5, 15), result.getPersonalInfo().getLocalBirthDate());
        assertEquals("+1-555-555-5555", result.getPersonalInfo().getContact().getPhoneNumber());
        assertEquals("12345", result.getApvnt());
        assertEquals("67890", result.getECrisId());
        assertEquals("rp67890", result.getENaukaId());
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
    void shouldImportPersonWithBasicInfoWhenInfoIsValid() {
        // given
        var personDTO = new ImportPersonDTO();
        var personNameDTO = new PersonNameDTO();
        personNameDTO.setFirstname("Jane");
        personNameDTO.setLastname("Smith");
        personDTO.setPersonName(personNameDTO);
        personDTO.setContactEmail("jane.smith@example.com");
        personDTO.setSex(Sex.FEMALE);
        personDTO.setLocalBirthDate(LocalDate.of(1990, 3, 10));
        personDTO.setPhoneNumber("+1-444-444-4444");
        personDTO.setPlaceOfBirth("New York");
        personDTO.setAddressLine(new ArrayList<>());
        personDTO.setAddressCity(new ArrayList<>());
        personDTO.setBiography(new ArrayList<>());
        personDTO.setKeywords(new ArrayList<>());
        personDTO.setApvnt("23456");
        personDTO.setECrisId("78901");
        personDTO.setENaukaId("rp78901");
        personDTO.setOrcid("0000-0002-1825-0000");
        personDTO.setScopusAuthorId("11111111111");
        personDTO.setOrganisationUnitId(2);
        personDTO.setEmploymentPosition(EmploymentPosition.RESEARCH_ASSOCIATE);

        var personalInfo = new PersonalInfo();
        personalInfo.setLocalBirthDate(personDTO.getLocalBirthDate());

        var person = new Person();
        person.setName(new PersonName());
        person.setPersonalInfo(personalInfo);
        person.setApproveStatus(ApproveStatus.APPROVED);

        // when
        var employmentInstitution = new OrganisationUnit();
        when(organisationUnitService.findOrganisationUnitById(2)).thenReturn(employmentInstitution);
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(new HashSet<>());
        when(personRepository.save(any(Person.class))).thenReturn(person);

        // then
        var result = personService.importPersonWithBasicInfo(personDTO, true);
        assertNotNull(result);
        assertEquals("Jane", result.getName().getFirstname());
        assertEquals("Smith", result.getName().getLastname());
        assertEquals("jane.smith@example.com",
            result.getPersonalInfo().getContact().getContactEmail());
        assertEquals(Sex.FEMALE, result.getPersonalInfo().getSex());
        assertEquals(LocalDate.of(1990, 3, 10), result.getPersonalInfo().getLocalBirthDate());
        assertEquals("+1-444-444-4444", result.getPersonalInfo().getContact().getPhoneNumber());
        assertEquals("23456", result.getApvnt());
        assertEquals("78901", result.getECrisId());
        assertEquals("rp78901", result.getENaukaId());
        assertEquals("0000-0002-1825-0000", result.getOrcid());
        assertEquals("11111111111", result.getScopusAuthorId());
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        assertEquals(1, result.getInvolvements().size());
        var currentEmployment = result.getInvolvements().iterator().next();
        assertEquals(ApproveStatus.APPROVED, currentEmployment.getApproveStatus());
        assertEquals(InvolvementType.EMPLOYED_AT, currentEmployment.getInvolvementType());
        assertEquals(EmploymentPosition.RESEARCH_ASSOCIATE,
            ((Employment) currentEmployment).getEmploymentPosition());
    }


    @Test
    public void shouldSetPersonBiographyWithAnyData() {
        // given
        var person = new Person();
        var bio1 = new MultilingualContentDTO(1, "EN", "English content", 1);
        var bio2 = new MultilingualContentDTO(2, "FR", "Contenu français", 2);
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
        var keyword1 = new MultilingualContentDTO(1, "EN", "English content", 1);
        var keyword2 = new MultilingualContentDTO(2, "FR", "Contenu français", 2);
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
        person.getOtherNames().add(personName2);
        person.setName(personName1);
        person.setPersonalInfo(personalInfo);
        person.setApproveStatus(ApproveStatus.APPROVED);

        when(personRepository.findById(1)).thenReturn(Optional.of(person));
        when(personNameService.findOne(2)).thenReturn(personName2);
        when(personIndexRepository.findByDatabaseId(anyInt())).thenReturn(
            Optional.of(new PersonIndex()));

        // when
        personService.setPersonMainName(2, 1);

        // then
        verify(personRepository, times(2)).save(person);
        assertEquals(person.getName().getOtherName(), personName2.getOtherName());
    }

    @Test
    void setPersonOtherNames_shouldSetOtherNamesForPerson() {
        // given
        var personId = 1;
        var personNameDTOList = new ArrayList<PersonNameDTO>();
        personNameDTOList.add(new PersonNameDTO(null, "John", "Doe", "Smith", null, null));
        personNameDTOList.add(new PersonNameDTO(null, "Jane", "Marie", "Doe", null, null));

        var personToUpdate = new Person();
        personToUpdate.setId(personId);
        personToUpdate.setApproveStatus(ApproveStatus.APPROVED);
        personToUpdate.setName(new PersonName());
        personToUpdate.setPersonalInfo(new PersonalInfo());

        when(personRepository.findById(personId)).thenReturn(Optional.of(personToUpdate));

        // when
        personService.setPersonOtherNames(personNameDTOList, personId);

        // then
        verify(personRepository, times(1)).findById(personId);
        verify(personRepository, times(4)).save(personToUpdate);
    }

    @Test
    void setPersonOtherNames_shouldDeleteExistingOtherNames() {
        // given
        var personId = 1;
        var personNameDTOList = new ArrayList<PersonNameDTO>();
        personNameDTOList.add(new PersonNameDTO(null, "John", "Doe", "Smith", null, null));

        var personToUpdate = new Person();
        personToUpdate.setId(personId);
        personToUpdate.setApproveStatus(ApproveStatus.APPROVED);
        personToUpdate.setName(new PersonName());
        personToUpdate.setPersonalInfo(new PersonalInfo());

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
        personNameDTOList.add(new PersonNameDTO(null, "John", "Doe", "Smith", null, null));

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
        personalInfoDTO.setPlaceOfBirth("City");
        personalInfoDTO.setApvnt("123123");
        personalInfoDTO.setECrisId("67890");
        personalInfoDTO.setENaukaId("aa123456");
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
        personalInfo.setPostalAddress(postalAddress);

        var contact = new Contact();
        personalInfo.setContact(contact);

        var personToUpdate = new Person();
        personToUpdate.setId(personId);
        personToUpdate.setPersonalInfo(personalInfo);
        personToUpdate.setName(new PersonName());
        personToUpdate.setApproveStatus(ApproveStatus.APPROVED);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personToUpdate));
        when(countryService.findOne(anyInt())).thenReturn(new Country());
        when(personRepository.save(any(Person.class))).thenReturn(personToUpdate);
        when(personIndexRepository.findByDatabaseId(anyInt())).thenReturn(
            Optional.of(new PersonIndex()));

        // when
        personService.updatePersonalInfo(personId, personalInfoDTO);

        // then
        verify(personRepository, times(1)).findById(personId);
        verify(personRepository, times(2)).save(personToUpdate);
        assertEquals("123123", personToUpdate.getApvnt());
        assertEquals("67890", personToUpdate.getECrisId());
        assertEquals("aa123456", personToUpdate.getENaukaId());
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
            personService.updatePersonalInfo(personId, personalInfoDTO);
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFindPeopleByNameAndEmploymentWhenProperQueryIsGiven() {
        // given
        var tokens = Arrays.asList("Ivan", "FTN");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new PersonIndex(), new PersonIndex())));

        // when
        var result = personService.findPeopleByNameAndEmployment(tokens, pageable, false, 0, false);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFindPeopleForOrganisationUnitWhenGivenValidId(boolean fetchAlumni) {
        // given
        var employmentInstitutionId = 123;
        var pageable = PageRequest.of(0, 10);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
            employmentInstitutionId)).thenReturn(List.of(employmentInstitutionId));
        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new PersonIndex())));

        // when
        var result =
            personService.findPeopleForOrganisationUnit(employmentInstitutionId, List.of("*"),
                pageable, fetchAlumni);

        // then
        assertEquals(result.getTotalElements(), 1L);
    }

    @Test
    public void shouldFindPersonWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("name:\"Ime Prezime\"", "employments_sr:ФТН");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new PersonIndex(), new PersonIndex())));

        // when
        var result = personService.advancedSearch(new ArrayList<>(tokens), pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldGetResearcherCount() {
        // Given
        var expectedCount = 42L;
        when(personIndexRepository.count()).thenReturn(expectedCount);

        // When
        long actualCount = personService.getResearcherCount();

        // Then
        assertEquals(expectedCount, actualCount);
        verify(personIndexRepository, times(1)).count();
    }

    @ParameterizedTest
    @CsvSource({
        "1, true, true",
        "2, true, false",
        "3, false, true",
    })
    void shouldThrowReferenceConstraintViolationExceptionWhenPersonIsUsed(Integer personId,
                                                                          boolean hasProjectContribution,
                                                                          boolean isBoundToUser) {
        // Given
        when(personRepository.hasContribution(personId)).thenReturn(hasProjectContribution);
        when(personRepository.isBoundToUser(personId)).thenReturn(isBoundToUser);

        // When
        assertThrows(PersonReferenceConstraintViolationException.class,
            () -> personService.deletePerson(personId));

        // Then (PersonReferenceConstraintViolationException should be thrown)
        verify(personRepository, never()).findById(personId);
        verify(personRepository, never()).save(any());
        verify(personIndexRepository, never()).delete(any());
    }

    @Test
    void shouldDeleteUnusedPerson() {
        // Given
        var personId = 5;
        when(personRepository.hasContribution(personId)).thenReturn(false);
        when(personRepository.isBoundToUser(personId)).thenReturn(false);
        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person()));
        when(personIndexRepository.findByDatabaseId(personId)).thenReturn(
            Optional.of(new PersonIndex()));

        // When
        personService.deletePerson(personId);

        // Then
        verify(personRepository, times(1)).findById(personId);
        verify(personRepository, times(1)).save(any());
        verify(personIndexRepository, times(1)).delete(any());
    }

    @Test
    public void shouldReindexPersons() {
        // Given
        var person1 = new Person();
        person1.setName(new PersonName());
        person1.setPersonalInfo(new PersonalInfo());
        var person2 = new Person();
        person2.setName(new PersonName());
        person2.setPersonalInfo(new PersonalInfo());
        var person3 = new Person();
        person3.setName(new PersonName());
        person3.setPersonalInfo(new PersonalInfo());
        var persons = Arrays.asList(person1, person2, person3);
        var page1 = new PageImpl<>(persons.subList(0, 2), PageRequest.of(0, 10), persons.size());
        var page2 = new PageImpl<>(persons.subList(2, 3), PageRequest.of(1, 10), persons.size());

        when(personRepository.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        personService.reindexPersons();

        // Then
        verify(personIndexRepository, times(1)).deleteAll();
        verify(personRepository, atLeastOnce()).findAll(any(PageRequest.class));
        verify(personIndexRepository, atLeastOnce()).save(any(PersonIndex.class));
    }

    @Test
    void shouldFindPersonByOldId() {
        // Given
        var oldId = 123;
        var expectedPerson = new Person();
        when(personRepository.findPersonByOldIdsContains(oldId)).thenReturn(
            Optional.of(expectedPerson));

        // When
        var actualPerson = personService.findPersonByOldId(oldId);

        // Then
        assertEquals(expectedPerson, actualPerson);
        verify(personRepository, times(1)).findPersonByOldIdsContains(oldId);
    }

    @Test
    void shouldReturnNullWhenOldIdDoesNotExist() {
        // Given
        var oldId = 123;
        when(personRepository.findPersonByOldIdsContains(oldId)).thenReturn(Optional.empty());

        // When
        var actualPerson = personService.findPersonByOldId(oldId);

        // Then
        assertNull(actualPerson);
        verify(personRepository, times(1)).findPersonByOldIdsContains(oldId);
    }

    @Test
    public void shouldGetPersonIdForUserId() {
        // Given
        var userId = 1;
        var expectedPersonId = 100;

        when(personRepository.findPersonIdForUserId(userId)).thenReturn(
            Optional.of(expectedPersonId));

        // When
        var actualPersonId = personService.getPersonIdForUserId(userId);

        // Then
        assertEquals(expectedPersonId, actualPersonId);
    }

    @Test
    public void shouldNotTestGetPersonIdForUserIdWhenNotPresent() {
        // Given
        var userId = 1;

        when(personRepository.findPersonIdForUserId(userId)).thenReturn(Optional.empty());

        // When
        var actualPersonId = personService.getPersonIdForUserId(userId);

        // Then
        assertNull(actualPersonId);
    }

    @Test
    public void testFindPersonByScopusAuthorId_PersonExists() {
        // Given
        var person = new PersonIndex();
        person.setScopusAuthorId("12345");

        when(personIndexRepository.findByScopusAuthorIdOrOpenAlexId("12345")).thenReturn(
            Optional.of(person));

        // When
        var foundPerson = personService.findPersonByImportIdentifier("12345");

        // Then
        assertEquals(person, foundPerson);
    }

    @Test
    public void testFindPersonByScopusAuthorId_PersonDoesNotExist() {
        // Given
        when(personIndexRepository.findByScopusAuthorIdOrOpenAlexId("12345")).thenReturn(
            Optional.empty());

        // When
        var foundPerson = personService.findPersonByImportIdentifier("12345");

        // Then
        assertNull(foundPerson);
    }

    @Test
    void shouldForceDeletePerson() {
        // Given
        var personId = 1;

        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person()));
        when(personRepository.isBoundToUser(personId)).thenReturn(false);
        when(personContributionRepository.fetchAllPersonDocumentContributions(eq(personId), any()))
            .thenReturn(Page.empty());
        when(personIndexRepository.findByDatabaseId(personId))
            .thenReturn(Optional.of(new PersonIndex()));

        // When
        personService.forceDeletePerson(personId);

        // Then
        verify(personRepository, times(1)).isBoundToUser(personId);
        verify(personContributionRepository, times(1)).deletePersonEventContributions(personId);
        verify(personContributionRepository, times(1)).deletePersonPublicationsSeriesContributions(
            personId);
        verify(personContributionRepository, times(1)).fetchAllPersonDocumentContributions(
            eq(personId), any());
        verify(personIndexRepository, times(1)).delete(any(PersonIndex.class));
        verify(documentPublicationIndexRepository, times(7)).deleteByAuthorIdsAndType(anyInt(),
            anyString());
    }

    @Test
    void switchToUnmanagedEntityShouldThrowsExceptionWhenPersonIsBoundToUser() {
        // Given
        var personId = 1;

        when(personRepository.isBoundToUser(personId)).thenReturn(true);

        // When
        var exception = assertThrows(PersonReferenceConstraintViolationException.class,
            () -> personService.switchToUnmanagedEntity(personId));

        // Then
        assertEquals("This person is already in use.", exception.getMessage());
        verify(personRepository, times(1)).isBoundToUser(personId);
        verifyNoInteractions(personContributionRepository, personIndexRepository,
            documentPublicationIndexRepository);
    }

    @Test
    void shouldSwitchToUnmanagedEntity() {
        // Given
        var personId = 1;

        when(personRepository.isBoundToUser(personId)).thenReturn(false);
        when(personIndexRepository.findByDatabaseId(personId)).thenReturn(Optional.empty());
        when(personContributionRepository.fetchAllPersonDocumentContributions(eq(1),
            any())).thenReturn(Page.empty());
        when(personRepository.findById(1)).thenReturn(Optional.of(new Person()));

        doNothing().when(personContributionRepository)
            .deleteInstitutionsForForPersonContributions(personId);
        doNothing().when(personContributionRepository)
            .makePersonEventContributionsPointToExternalContributor(personId);
        doNothing().when(personContributionRepository)
            .makePersonPublicationsSeriesContributionsPointToExternalContributor(personId);

        // When
        personService.switchToUnmanagedEntity(personId);

        // Then
        verify(personRepository, times(1)).isBoundToUser(personId);
        verify(personContributionRepository, times(1)).deleteInstitutionsForForPersonContributions(
            personId);
        verify(personContributionRepository,
            times(1)).makePersonEventContributionsPointToExternalContributor(personId);
        verify(personContributionRepository,
            times(1)).makePersonPublicationsSeriesContributionsPointToExternalContributor(personId);
        verify(personIndexRepository, times(1)).findByDatabaseId(personId);
        verifyNoInteractions(documentPublicationIndexRepository);
    }

    @Test
    void shouldScanDataSourcesWhenScopusAuthorIdIsNonEmpty() {
        var personId = 1;
        var person = new Person();
        person.setScopusAuthorId("1234");

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        var response = personService.canPersonScanDataSources(personId);

        assertTrue(response);
    }

    @Test
    void shouldReturnFalseWhenScopusAuthorIdIsEmpty() {
        var personId = 1;

        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person()));

        var response = personService.canPersonScanDataSources(personId);

        assertFalse(response);
    }

    @Test
    void shouldUpdateAndIndexPersonPrimaryNameWhenStatusIsApproved() {
        // Given
        var personId = 1;
        var personNameDTO = new PersonNameDTO(null, "John", "Michael", "Doe", null, null);
        var person = new Person();
        person.setId(personId);
        person.setPersonalInfo(new PersonalInfo());
        person.setName(new PersonName("OldFirst", "OldOther", "OldLast", null, null));
        person.setApproveStatus(ApproveStatus.APPROVED);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        personService.updatePersonMainName(personId, personNameDTO);

        // Then
        assertEquals("John", person.getName().getFirstname());
        assertEquals("Michael", person.getName().getOtherName());
        assertEquals("Doe", person.getName().getLastname());

        verify(personRepository, times(2)).save(person);
    }

    @Test
    void shouldUpdateButNotIndexPersonPrimaryNameWhenStatusIsNotApproved() {
        // Given
        var personId = 2;
        var personNameDTO = new PersonNameDTO(null, "Jane", "Alice", "Smith", null, null);
        var person = new Person();
        person.setId(personId);
        person.setName(new PersonName("OldFirst", "OldOther", "OldLast", null, null));
        person.setApproveStatus(ApproveStatus.REQUESTED);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // When
        personService.updatePersonMainName(personId, personNameDTO);

        // Then
        assertEquals("Jane", person.getName().getFirstname());
        assertEquals("Alice", person.getName().getOtherName());
        assertEquals("Smith", person.getName().getLastname());

        verify(personRepository).save(person);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPersonNotFound() {
        // Given
        var personId = 3;
        var personNameDTO = new PersonNameDTO(null, "Test", "User", "Test", null, null);
        when(personRepository.findById(personId)).thenThrow(
            new NotFoundException("Person not found"));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            personService.updatePersonMainName(personId, personNameDTO);
        });

        verify(personRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForInvalidMimeType() {
        // given
        var personId = 1;
        var mockFile = createMockMultipartFile();

        // when / then
        assertThrows(IllegalArgumentException.class,
            () -> personService.setPersonProfileImage(personId,
                new ProfilePhotoOrLogoDTO(1, 2, 3, 4, "", mockFile)));
        verifyNoInteractions(fileService);
    }

    @Test
    void shouldReturnTrueWhenIdentifierExistsInOrcid() {
        // Given
        String identifier = "0000-0001-2345-6789";
        Integer personId = 1;

        when(personRepository.existsByOrcid(identifier, personId)).thenReturn(true);

        // When
        boolean result = personService.isIdentifierInUse(identifier, personId);

        // Then
        assertTrue(result);
        verify(personRepository).existsByOrcid(identifier, personId);
    }

    @Test
    void shouldReturnTrueWhenIdentifierExistsInScopus() {
        // Given
        String identifier = "123456789";
        Integer personId = 1;

        when(personRepository.existsByScopusAuthorId(identifier, personId)).thenReturn(true);

        // When
        boolean result = personService.isIdentifierInUse(identifier, personId);

        // Then
        assertTrue(result);
        verify(personRepository).existsByScopusAuthorId(identifier, personId);
    }

    @Test
    void shouldReturnTrueWhenIdentifierExistsInOtherRepositories() {
        // Given
        String identifier = "ecris-987";
        Integer personId = 2;

        when(personRepository.existsByeCrisId(identifier, personId)).thenReturn(true);

        // When
        boolean result = personService.isIdentifierInUse(identifier, personId);

        // Then
        assertTrue(result);
        verify(personRepository).existsByeCrisId(identifier, personId);
    }

    @Test
    void shouldReturnFalseWhenIdentifierDoesNotExistAnywhere() {
        // Given
        String identifier = "9999-0000";
        Integer personId = 3;

        when(personRepository.existsByOrcid(identifier, personId)).thenReturn(false);
        when(personRepository.existsByScopusAuthorId(identifier, personId)).thenReturn(false);
        when(personRepository.existsByeCrisId(identifier, personId)).thenReturn(false);
        when(personRepository.existsByeNaukaId(identifier, personId)).thenReturn(false);

        // When
        boolean result = personService.isIdentifierInUse(identifier, personId);

        // Then
        assertFalse(result);
        verify(personRepository).existsByOrcid(identifier, personId);
        verify(personRepository).existsByScopusAuthorId(identifier, personId);
        verify(personRepository).existsByeCrisId(identifier, personId);
        verify(personRepository).existsByeNaukaId(identifier, personId);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnSearchFields(Boolean onlyExportFields) {
        // Given
        var expectedFields = List.of(
            new Triple<>("field1", List.of(new MultilingualContentDTO()), "Type1"),
            new Triple<>("field2", List.of(new MultilingualContentDTO()), "Type2")
        );

        when(searchFieldsLoader.getSearchFields(any(), anyBoolean())).thenReturn(expectedFields);

        // When
        var result = personService.getSearchFields(onlyExportFields);

        // Then
        assertNotNull(result);
        assertEquals(expectedFields.size(), result.size());
    }

    private MultipartFile createMockMultipartFile() {
        return new MockMultipartFile("file", "test.txt", "text/plain",
            "Test file content".getBytes());
    }

    private MultipartFile createMockMultipartFile(byte[] content) {
        return new MockMultipartFile("file", "test.txt", "text/plain", content);
    }

    @Test
    void shouldReturnPersonByAccountingId() {
        // Given
        var accountingId = "ACC123";
        var expectedPerson = new Person();
        expectedPerson.setAccountingIds(Set.of(accountingId));

        when(personRepository.findApprovedPersonByAccountingId(accountingId))
            .thenReturn(Optional.of(expectedPerson));

        // When
        var actualPerson = personService.findPersonByAccountingId(accountingId);

        // Then
        assertEquals(expectedPerson, actualPerson);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAccountingIdIsNotFound() {
        // Given
        var accountingId = "MISSING_ID";
        when(personRepository.findApprovedPersonByAccountingId(accountingId))
            .thenReturn(Optional.empty());

        // When / Then
        var ex = assertThrows(NotFoundException.class, () ->
            personService.findPersonByAccountingId(accountingId)
        );
        assertEquals("Person with accounting ID MISSING_ID does not exist", ex.getMessage());
    }

    @Test
    void shouldRemoveProfilePhotoWhenImageExists() {
        // given
        var personId = 1;
        var profilePhoto = new ProfilePhotoOrLogo();
        profilePhoto.setImageServerName("photo.jpg");
        profilePhoto.setTopOffset(10);
        profilePhoto.setLeftOffset(10);
        profilePhoto.setHeight(100);
        profilePhoto.setWidth(100);

        var person = new Person();
        person.setId(personId);
        person.setProfilePhoto(profilePhoto);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // when
        personService.removePersonProfileImage(personId);

        // then
        verify(fileService).delete("photo.jpg");
        assertNull(profilePhoto.getImageServerName());
        assertNull(profilePhoto.getTopOffset());
        assertNull(profilePhoto.getLeftOffset());
        assertNull(profilePhoto.getHeight());
        assertNull(profilePhoto.getWidth());
        verify(personRepository).save(person);
    }

    @Test
    void shouldDoNothingWhenNoProfilePhotoExists() {
        // given
        var personId = 1;
        var person = new Person();
        person.setId(personId);
        person.setProfilePhoto(null); // no photo

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        // when
        personService.removePersonProfileImage(personId);

        // then
        verifyNoInteractions(fileService);
        verify(personRepository).save(person);
    }

    @Test
    void shouldSetProfileImageAndSavePerson() throws IOException {
        // given
        var personId = 1;
        var mockFile = createMockMultipartFile();
        var profilePhotoDTO = new ProfilePhotoOrLogoDTO(10, 20, 100, 150, "", mockFile);
        var person = new Person();
        person.setId(personId);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(fileService.store(any(), anyString())).thenReturn("stored-file.jpg");

        try (MockedStatic<ImageUtil> imageUtil = mockStatic(ImageUtil.class)) {
            imageUtil.when(() -> ImageUtil.isMIMETypeInvalid(mockFile, false)).thenReturn(false);

            // when
            var result = personService.setPersonProfileImage(personId, profilePhotoDTO);

            // then
            assertEquals("stored-file.jpg", result);
            assertNotNull(person.getProfilePhoto());
            assertEquals(20, person.getProfilePhoto().getTopOffset());
            assertEquals(10, person.getProfilePhoto().getLeftOffset());
            assertEquals(150, person.getProfilePhoto().getHeight());
            assertEquals(100, person.getProfilePhoto().getWidth());
            assertEquals("stored-file.jpg", person.getProfilePhoto().getImageServerName());

            verify(fileService).store(eq(mockFile), anyString());
            verify(personRepository).save(person);
        }
    }

    @Test
    void shouldDeleteOldImageWhenNewOneIsUploaded() throws IOException {
        // given
        var personId = 1;
        var mockFile = createMockMultipartFile();
        var dto = new ProfilePhotoOrLogoDTO(1, 2, 3, 4, "", mockFile);
        var existingPhoto = new ProfilePhotoOrLogo();
        existingPhoto.setImageServerName("old-image.jpg");

        var person = new Person();
        person.setId(personId);
        person.setProfilePhoto(existingPhoto);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(fileService.store(any(), anyString())).thenReturn("new-image.jpg");

        try (MockedStatic<ImageUtil> imageUtil = mockStatic(ImageUtil.class)) {
            imageUtil.when(() -> ImageUtil.isMIMETypeInvalid(mockFile, false)).thenReturn(false);

            // when
            var result = personService.setPersonProfileImage(personId, dto);

            // then
            assertEquals("new-image.jpg", result);
            verify(fileService).delete("old-image.jpg");
            verify(fileService).store(eq(mockFile), anyString());
            verify(personRepository).save(person);
        }
    }

    @Test
    void shouldNotDeleteOrStoreFileWhenFileIsEmpty() throws IOException {
        // given
        var personId = 1;
        var mockFile = createMockMultipartFile(new byte[0]); // empty file
        var dto = new ProfilePhotoOrLogoDTO(5, 6, 7, 8, "", mockFile);
        var existingPhoto = new ProfilePhotoOrLogo();
        existingPhoto.setImageServerName("unchanged.jpg");

        var person = new Person();
        person.setId(personId);
        person.setProfilePhoto(existingPhoto);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));

        try (MockedStatic<ImageUtil> imageUtil = mockStatic(ImageUtil.class)) {
            imageUtil.when(() -> ImageUtil.isMIMETypeInvalid(mockFile, false)).thenReturn(false);

            // when
            var result = personService.setPersonProfileImage(personId, dto);

            // then
            assertEquals("unchanged.jpg", result);
            assertEquals(6, existingPhoto.getTopOffset());
            assertEquals(5, existingPhoto.getLeftOffset());
            assertEquals(8, existingPhoto.getHeight());
            assertEquals(7, existingPhoto.getWidth());

            verify(fileService, never()).delete(any());
            verify(fileService, never()).store(any(), anyString());
            verify(personRepository).save(person);
        }
    }

    @Test
    void testFindPersonsByLRUHarvest() {
        // Given
        var pageable = PageRequest.of(0, 10);
        var mockPeople = List.of(new Person(), new Person());
        var mockPage = new PageImpl<>(mockPeople, pageable, mockPeople.size());

        when(personRepository.findPersonsByLRUHarvest(pageable)).thenReturn(mockPage);

        // When
        Page<Person> result = personService.findPersonsByLRUHarvest(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(mockPage, result);
        verify(personRepository, times(1)).findPersonsByLRUHarvest(pageable);
    }

    @Test
    void shouldReturnRawPerson() {
        // Given
        var entityId = 123;
        var expected = new Person();
        expected.setId(entityId);
        when(personRepository.findRaw(entityId)).thenReturn(Optional.of(expected));

        // When
        var actual = personService.findRaw(entityId);

        // Then
        assertEquals(expected, actual);
        verify(personRepository).findRaw(entityId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenPersonDoesNotExist() {
        // Given
        var entityId = 123;
        when(personRepository.findRaw(entityId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> personService.findRaw(entityId));

        assertEquals("Person with given ID does not exist.", exception.getMessage());
        verify(personRepository).findRaw(entityId);
    }

    @Test
    void shouldReturnEmptyWithNullIdentifier() {
        // When
        var result = personService.findPersonByIdentifier(null);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(personRepository);
    }

    @Test
    void shouldReturnEmptyWithBlankIdentifier() {
        // When
        var result = personService.findPersonByIdentifier("   ");

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(personRepository);
    }

    @Test
    void shouldReturnPersonWithValidIdentifier() {
        // Given
        var identifier = "ORCID-1234";
        var person = new Person();
        when(personRepository.findPersonForIdentifier(identifier)).thenReturn(Optional.of(person));

        // When
        var result = personService.findPersonByIdentifier(identifier);

        // Then
        assertThat(result).contains(person);
        verify(personRepository).findPersonForIdentifier(identifier);
    }

    @Test
    void shouldReturnEmptyWithNonExistingIdentifier() {
        // Given
        var identifier = "SCOPUS-5678";
        when(personRepository.findPersonForIdentifier(identifier)).thenReturn(Optional.empty());

        // When
        var result = personService.findPersonByIdentifier(identifier);

        // Then
        assertThat(result).isEmpty();
        verify(personRepository).findPersonForIdentifier(identifier);
    }
}
