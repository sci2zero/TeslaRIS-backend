package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
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
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
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

    @InjectMocks
    private RegistryBookServiceImpl service;


    @Test
    void shouldCreateRegistryBookEntry() {
        // Given
        var dto = new RegistryBookEntryDTO();
        dto.setPromotionId(1);

        var dissertationInfo = new DissertationInformationDTO();
        dissertationInfo.setDissertationTitle(
            List.of(new MultilingualContentDTO(1, "en", "Title", 1)));
        dissertationInfo.setOrganisationUnitId(2);
        dissertationInfo.setMentor("Mentor");
        dissertationInfo.setCommission("Commission");
        dissertationInfo.setGrade(5);
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

        when(registryBookEntryRepository.save(any(RegistryBookEntry.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        // When
        RegistryBookEntry result = service.createRegistryBookEntry(dto);

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
        dto.setPromotionId(5);
        dto.setDissertationInformation(new DissertationInformationDTO());
        dto.setPersonalInformation(personalInfo);
        dto.setContactInformation(contactInfo);
        dto.setPreviousTitleInformation(new PreviousTitleInformationDTO());

        when(registryBookEntryRepository.findById(id)).thenReturn(Optional.of(entry));
        when(promotionService.findOne(5)).thenReturn(new Promotion());

        // When
        service.updateRegistryBookEntry(id, dto);

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
        service.deleteRegistryBookEntry(id);

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
        var result = service.getRegistryBookEntriesForPromotion(promotionId, pageable);

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
        var result = service.getPrePopulatedPHDThesisInformation(1);

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
            service.getPrePopulatedPHDThesisInformation(1));
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
            service.getPrePopulatedPHDThesisInformation(1));
    }
}
