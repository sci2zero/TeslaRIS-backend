package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PersonalTitle;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.thesislibrary.converter.RegistryBookEntryConverter;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.DissertationInformation;
import rs.teslaris.thesislibrary.model.PreviousTitleInformation;
import rs.teslaris.thesislibrary.model.Promotion;
import rs.teslaris.thesislibrary.model.RegistryBookContactInformation;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;
import rs.teslaris.thesislibrary.model.RegistryBookPersonalInformation;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryRepository;
import rs.teslaris.thesislibrary.service.impl.RegistryBookServiceImpl;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@SpringBootTest
class RegistryBookServiceTest {

    @Mock
    private RegistryBookEntryRepository registryBookEntryRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CountryService countryService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private ThesisService thesisService;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private RegistryBookServiceImpl registryBookService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(registryBookService, "clientAppAddress",
            "protocol://test.test/");
    }

    @Test
    void shouldCreateRegistryBookEntry() {
        // Given
        var dto = new RegistryBookEntryDTO();

        var dissertationInfo = new DissertationInformationDTO();
        dissertationInfo.setDissertationTitle("Dissertation title");
        dissertationInfo.setOrganisationUnitId(2);
        dissertationInfo.setMentor("Mentor");
        dissertationInfo.setCommission("Commission");
        dissertationInfo.setGrade("A");
        dissertationInfo.setAcquiredTitle("PhD");
        dissertationInfo.setDefenceDate(LocalDate.now());
        dissertationInfo.setDiplomaNumber("123");
        dissertationInfo.setDiplomaIssueDate(LocalDate.now());
        dissertationInfo.setDiplomaSupplementsNumber("456");
        dissertationInfo.setDiplomaSupplementsIssueDate(LocalDate.now());
        dto.setDissertationInformation(dissertationInfo);

        var personalInfo = new RegistryBookPersonalInformationDTO();
        var name = new PersonNameDTO(null, "John", "H", "Doe", null, null);
        personalInfo.setAuthorName(name);
        personalInfo.setLocalBirthDate(LocalDate.of(1990, 1, 1));
        personalInfo.setPlaceOfBrith("City");
        personalInfo.setMunicipalityOfBrith("Municipality");
        personalInfo.setCountryOfBirthId(3);
        personalInfo.setFatherName("Father");
        personalInfo.setFatherSurname("Doe Sr.");
        personalInfo.setMotherName("Mother");
        personalInfo.setMotherSurname("Smith");
        personalInfo.setGuardianNameAndSurname("Uncle Joe");
        dto.setPersonalInformation(personalInfo);

        var contactInfo = new RegistryBookContactInformationDTO();
        contactInfo.setResidenceCountryId(4);
        contactInfo.setStreetAndNumber("Main 1");
        contactInfo.setPlace("Place");
        contactInfo.setMunicipality("Municipality");
        contactInfo.setPostalCode("10000");
        contactInfo.setContact(new ContactDTO("john@example.com", "+123456"));
        dto.setContactInformation(contactInfo);

        var prevTitle = new PreviousTitleInformationDTO();
        prevTitle.setInstitutionName("University");
        prevTitle.setGraduationDate(LocalDate.of(2015, 6, 15));
        prevTitle.setInstitutionPlace("Town");
        prevTitle.setSchoolYear("2014/2015");
        dto.setPreviousTitleInformation(prevTitle);

        when(promotionService.findOne(1)).thenReturn(new Promotion());
        when(organisationUnitService.findOne(2)).thenReturn(new OrganisationUnit());
        when(countryService.findOne(3)).thenReturn(new Country());
        when(countryService.findOne(4)).thenReturn(new Country());
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(thesisService.getThesisById(1)).thenReturn(new Thesis());

        when(registryBookEntryRepository.save(any(RegistryBookEntry.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        // When
        RegistryBookEntry result = registryBookService.createRegistryBookEntry(dto, 1);

        // Then
        assertNotNull(result);
        verify(registryBookEntryRepository).save(any());
    }

    @Test
    void shouldUpdateRegistryBookEntry() {
        // Given
        var personalInfo = new RegistryBookPersonalInformationDTO();
        personalInfo.setAuthorName(
            new PersonNameDTO(null, "Ivan", "Radomir", "Mrsulja", null, null));
        var contactInfo = new RegistryBookContactInformationDTO();
        contactInfo.setContact(new ContactDTO());

        var id = 1;
        RegistryBookEntry entry = new RegistryBookEntry();
        RegistryBookEntryDTO dto = new RegistryBookEntryDTO();
        dto.setDissertationInformation(new DissertationInformationDTO());
        dto.setPersonalInformation(personalInfo);
        dto.setContactInformation(contactInfo);
        dto.setPreviousTitleInformation(new PreviousTitleInformationDTO());

        when(registryBookEntryRepository.findById(id)).thenReturn(Optional.of(entry));
        when(promotionService.findOne(5)).thenReturn(new Promotion());

        // When
        registryBookService.updateRegistryBookEntry(id, dto);

        // Then
        verify(registryBookEntryRepository).save(entry);
    }

    @Test
    void shouldDeleteRegistryBookEntry() {
        // Given
        var id = 10;
        RegistryBookEntry entry = new RegistryBookEntry();
        when(registryBookEntryRepository.findById(id)).thenReturn(Optional.of(entry));

        // When
        registryBookService.deleteRegistryBookEntry(id);

        // Then
        verify(registryBookEntryRepository).delete(entry);
    }

    @Test
    void shouldGetRegistryBookEntriesForPromotion() {
        // Given
        var promotionId = 1;
        var promotion = new Promotion();
        promotion.setId(promotionId);
        var pageable = PageRequest.of(0, 2);

        RegistryBookEntry entry1 = new RegistryBookEntry();
        entry1.setId(101);
        entry1.setPromotion(promotion);
        entry1.setDissertationInformation(new DissertationInformation());
        entry1.setContactInformation(new RegistryBookContactInformation());
        entry1.setPersonalInformation(new RegistryBookPersonalInformation());
        entry1.setPreviousTitleInformation(new PreviousTitleInformation());
        RegistryBookEntry entry2 = new RegistryBookEntry();
        entry2.setId(102);
        entry2.setPromotion(promotion);
        entry2.setDissertationInformation(new DissertationInformation());
        entry2.setContactInformation(new RegistryBookContactInformation());
        entry2.setPersonalInformation(new RegistryBookPersonalInformation());
        entry2.setPreviousTitleInformation(new PreviousTitleInformation());

        var page = new PageImpl<>(List.of(entry1, entry2), pageable, 2);
        when(registryBookEntryRepository.getBookEntriesForPromotion(promotionId,
            pageable)).thenReturn(page);

        // When
        var result = registryBookService.getRegistryBookEntriesForPromotion(promotionId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(Objects::nonNull));
        verify(registryBookEntryRepository).getBookEntriesForPromotion(promotionId, pageable);
    }

    @Test
    void shouldReturnPrePopulatedPhdThesisInformationWhenValidPhdThesisExists() {
        // Given
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.PHD);
        thesis.setThesisDefenceDate(LocalDate.of(2024, 6, 1));

        var author = new PersonDocumentContribution();
        author.setContributionType(DocumentContributionType.AUTHOR);
        var authorPerson = new Person();
        var personalInfo = new PersonalInfo();
        personalInfo.setLocalBirthDate(LocalDate.of(1990, 1, 1));
        personalInfo.setPlaceOfBrith("Novi Sad");
        personalInfo.setPostalAddress(new PostalAddress());
        personalInfo.setContact(new Contact("email@example.com", "+381111111"));
        authorPerson.setPersonalInfo(personalInfo);
        author.setPerson(authorPerson);
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setDisplayPersonName(
            new PersonName("Ime", null, "Prezime", null, null));
        author.setAffiliationStatement(affiliationStatement);

        var advisor = new PersonDocumentContribution();
        advisor.setContributionType(DocumentContributionType.ADVISOR);
        advisor.setPersonalTitle(PersonalTitle.MR);
        advisor.setEmploymentTitle(EmploymentTitle.ASSOCIATE_PROFESSOR);
        var affiliationStatement2 = new AffiliationStatement();
        affiliationStatement2.setDisplayPersonName(
            new PersonName("Mentor", null, "Prezime", null, null));
        advisor.setAffiliationStatement(affiliationStatement2);
        var advisorInstitution = new OrganisationUnit();
        advisorInstitution.setName(Set.of());
        advisorInstitution.setLocation(new GeoLocation());
        advisor.setInstitutions(Set.of(advisorInstitution));

        thesis.setContributors(Set.of(author, advisor));

        thesis.setTitle(Set.of());
        thesis.setOrganisationUnit(advisorInstitution);
        thesis.getOrganisationUnit().setName(Set.of());

        when(thesisService.getThesisById(1)).thenReturn(thesis);

        // When
        var result = registryBookService.getPrePopulatedPHDThesisInformation(1);

        // Then
        assertEquals(LocalDate.of(1990, 1, 1), result.getLocalBirthDate());
        assertEquals("Novi Sad", result.getPlaceOfBirth());
        assertEquals("мр Mentor Prezime, ванр. проф.", result.getMentor());
        assertEquals("email@example.com", result.getContact().getContactEmail());
        assertEquals("+381111111", result.getContact().getPhoneNumber());
    }

    @Test
    void shouldThrowThesisExceptionWhenThesisDoesNotHaveDefenceDate() {
        // Given
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.PHD);
        thesis.setThesisDefenceDate(null);
        when(thesisService.getThesisById(1)).thenReturn(thesis);

        // Then
        assertThrows(ThesisException.class, () ->
            registryBookService.getPrePopulatedPHDThesisInformation(1));
    }

    @Test
    void shouldThrowThesisExceptionWhenThesisTypeIsNotPHDOrArtProject() {
        // Given
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.BACHELOR);
        thesis.setThesisDefenceDate(LocalDate.of(2024, 6, 1));
        when(thesisService.getThesisById(1)).thenReturn(thesis);

        // Then
        assertThrows(ThesisException.class, () ->
            registryBookService.getPrePopulatedPHDThesisInformation(1));
    }

    @Test
    void shouldAddToPromotion() {
        // Given
        var entryId = 1;
        var promotionId = 2;
        var entry = new RegistryBookEntry();
        var personalInfo = new RegistryBookPersonalInformation();
        personalInfo.setAuthorName(new PersonName("John", "Jane", "Doe", null, null));
        entry.setPersonalInformation(personalInfo);
        var contactInfo = new RegistryBookContactInformation();
        contactInfo.setContact(new Contact("email", "phone"));
        entry.setContactInformation(contactInfo);
        var promotion = new Promotion();

        when(registryBookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(promotionService.findOne(promotionId)).thenReturn(promotion);

        // When
        registryBookService.addToPromotion(entryId, promotionId);

        // Then
        assertEquals(promotion, entry.getPromotion());
    }

    @Test
    void shouldThrowExceptionWhenAlreadyAddedToPromotion() {
        // Given
        var entryId = 1;
        var entry = new RegistryBookEntry();
        entry.setPromotion(new Promotion());

        when(registryBookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When / Then
        assertThrows(PromotionException.class,
            () -> registryBookService.addToPromotion(entryId, 5));
    }

    @Test
    void shouldRemoveFromPromotion() {
        // Given
        var entryId = 1;
        var promotion = new Promotion();
        var entry = new RegistryBookEntry();
        var personalInfo = new RegistryBookPersonalInformation();
        personalInfo.setAuthorName(new PersonName("John", "Jane", "Doe", null, null));
        entry.setPersonalInformation(personalInfo);
        var contactInfo = new RegistryBookContactInformation();
        contactInfo.setContact(new Contact("email", "phone"));
        entry.setContactInformation(contactInfo);
        entry.setPromotion(promotion);

        when(registryBookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When
        registryBookService.removeFromPromotion(entryId);

        // Then
        assertNull(entry.getPromotion());
    }

    @Test
    void shouldThrowExceptionWhenNoPromotionSet() {
        // Given
        var entryId = 1;
        var entry = new RegistryBookEntry();

        when(registryBookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When / Then
        assertThrows(PromotionException.class,
            () -> registryBookService.removeFromPromotion(entryId));
    }

    @Test
    void shouldGetNonPromotedEntries() {
        // Given
        var pageable = PageRequest.of(0, 10);
        var entry = new RegistryBookEntry();
        entry.setDissertationInformation(new DissertationInformation());
        entry.setPersonalInformation(new RegistryBookPersonalInformation());
        entry.setContactInformation(new RegistryBookContactInformation());
        entry.setPreviousTitleInformation(new PreviousTitleInformation());
        var page = new PageImpl<>(List.of(entry));

        when(registryBookEntryRepository.getNonPromotedBookEntries(pageable)).thenReturn(page);

        // When
        var result = registryBookService.getNonPromotedRegistryBookEntries(pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        verify(registryBookEntryRepository).getNonPromotedBookEntries(pageable);
    }

    @Test
    void shouldPromoteAllEntriesAndMarkPromotionAsFinished() {
        // Given
        var promotionId = 1;
        var promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setFinished(false);
        promotion.setPromotionDate(LocalDate.of(2024, 4, 4));

        var entry1 = new RegistryBookEntry();
        var entry2 = new RegistryBookEntry();

        when(promotionService.findOne(promotionId)).thenReturn(promotion);
        when(registryBookEntryRepository.getLastRegistryBookNumber()).thenReturn(5);
        when(registryBookEntryRepository.getBookEntriesForPromotion(eq(promotionId), any()))
            .thenReturn(new PageImpl<>(List.of(entry1, entry2)));

        // When
        registryBookService.promoteAll(promotionId);

        // Then
        verify(registryBookEntryRepository, atLeastOnce()).save(entry1);
        verify(registryBookEntryRepository, atLeastOnce()).save(entry2);
        verify(promotionService).save(promotion);

        assertEquals(6, entry1.getRegistryBookNumber());
        assertEquals(7, entry2.getRegistryBookNumber());
        assertNull(entry1.getAttendanceIdentifier());
        assertNull(entry2.getAttendanceIdentifier());
        assertTrue(promotion.getFinished());
    }

    @Test
    void shouldReadRegistryBookEntryById() {
        // Given
        var entryId = 42;
        var entry = new RegistryBookEntry();
        var expectedDto = new RegistryBookEntryDTO();

        when(registryBookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        mockStatic(RegistryBookEntryConverter.class);
        when(RegistryBookEntryConverter.toDTO(any())).thenReturn(expectedDto);

        // When
        var result = registryBookService.readRegistryBookEntry(entryId);

        // Then
        assertEquals(expectedDto, result);
        verify(registryBookEntryRepository).findById(entryId);
    }

    @Test
    void shouldReturnTrueIfThesisHasRegistryBookEntry() {
        // Given
        var thesisId = 123;
        when(registryBookEntryRepository.hasThesisRegistryBookEntry(thesisId)).thenReturn(true);

        // When
        var result = registryBookService.hasThesisRegistryBookEntry(thesisId);

        // Then
        assertTrue(result);
        verify(registryBookEntryRepository).hasThesisRegistryBookEntry(thesisId);
    }
}
