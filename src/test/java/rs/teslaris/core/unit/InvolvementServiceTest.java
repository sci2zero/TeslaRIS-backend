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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.person.InternalIdentifierMigrationDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.ExtraEmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.person.InvolvementServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class InvolvementServiceTest {

    @Mock
    private PersonService personService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private InvolvementRepository involvementRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private UserService userService;

    @Mock
    private EmploymentRepository employmentRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private InvolvementServiceImpl involvementService;


    @Test
    public void shouldReturnInvolvementWhenInvolvementExists() {
        // given
        var expected = new Involvement();

        when(involvementRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var actual = involvementService.findOne(1);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenInvolvementDoesNotExist() {
        // given
        when(involvementRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> involvementService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReturnGenericInvolvementWhenInvolvementExists() {
        // given
        var mc1 = new MultiLingualContent(new LanguageTag(), "aaa", 1);
        var education = new Education();
        education.setOrganisationUnit(new OrganisationUnit());
        education.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        education.setThesisTitle(new HashSet<>(Set.of(mc1)));
        education.setTitle(new HashSet<>(Set.of(mc1)));
        education.setAbbreviationTitle(new HashSet<>(Set.of(mc1)));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(education));

        // when
        var actual = (EducationDTO) involvementService.getInvolvement(1, Education.class);

        // then
        assertNotNull(actual);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenGenericInvolvementDoesNotExist() {
        // given
        when(involvementRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> involvementService.getInvolvement(1, Education.class));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldAddEducationWithValidData() {
        // given
        var person = new Person();
        var educationDTO = new EducationDTO();
        var mc = new MultilingualContentDTO(1, "EN", "aaa", 1);
        educationDTO.setAffiliationStatement(List.of(mc));
        educationDTO.setThesisTitle(List.of(mc));
        educationDTO.setTitle(List.of(mc));
        educationDTO.setAbbreviationTitle(List.of(mc));

        when(personService.findOne(1)).thenReturn(person);
        when(involvementRepository.save(any())).thenReturn(new Education());

        // when
        var result = involvementService.addEducation(1, educationDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(result);
    }

    @Test
    public void shouldAddMembershipWithValidData() {
        // given
        var person = new Person();
        var membershipDTO = new MembershipDTO();
        var mc = new MultilingualContentDTO(1, "EN", "aaa", 1);
        membershipDTO.setAffiliationStatement(List.of(mc));
        membershipDTO.setRole(List.of(mc));
        membershipDTO.setContributionDescription(List.of(mc));

        when(personService.findOne(1)).thenReturn(person);
        when(involvementRepository.save(any())).thenReturn(new Membership());

        // when
        var result = involvementService.addMembership(1, membershipDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(any());
    }

    @Test
    public void shouldAddEmploymentWithValidData() {
        // given
        var person = new Person();
        var employmentDTO = new EmploymentDTO();
        var mc = new MultilingualContentDTO(1, "EN", "aaa", 1);
        employmentDTO.setAffiliationStatement(List.of(mc));
        employmentDTO.setRole(List.of(mc));

        when(personService.findOne(1)).thenReturn(person);
        when(involvementRepository.save(any())).thenReturn(new Employment());

        // when
        var result = involvementService.addEmployment(1, employmentDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateEducationWithValidData() {
        // given
        var mc1 = new MultiLingualContent(null, "aaa", 1);
        var education = new Education();
        education.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        education.setThesisTitle(new HashSet<>(Set.of(mc1)));
        education.setTitle(new HashSet<>(Set.of(mc1)));
        education.setAbbreviationTitle(new HashSet<>(Set.of(mc1)));
        education.setPersonInvolved(new Person());
        var educationDTO = new EducationDTO();
        var mc2 = new MultilingualContentDTO(1, "EN", "bbb", 1);
        educationDTO.setAffiliationStatement(List.of(mc2));
        educationDTO.setThesisTitle(List.of(mc2));
        educationDTO.setTitle(List.of(mc2));
        educationDTO.setAbbreviationTitle(List.of(mc2));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(education));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(Set.of(mc1));
        when(involvementRepository.save(any())).thenReturn(new Employment());

        // when
        involvementService.updateEducation(1, educationDTO);

        // then
        assertEquals(education.getThesisTitle().size(), 1);
        verify(involvementRepository, times(1)).save(education);
    }

    @Test
    public void shouldUpdateMembershipWithValidData() {
        // given
        var mc1 = new MultiLingualContent(null, "aaa", 1);
        var membership = new Membership();
        membership.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        membership.setContributionDescription(new HashSet<>(Set.of(mc1)));
        membership.setRole(new HashSet<>(Set.of(mc1)));
        membership.setPersonInvolved(new Person());
        var membershipDTO = new MembershipDTO();
        var mc2 = new MultilingualContentDTO(1, "EN", "bbb", 1);
        membershipDTO.setAffiliationStatement(List.of(mc2));
        membershipDTO.setRole(List.of(mc2));
        membershipDTO.setContributionDescription(List.of(mc2));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(membership));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(Set.of(mc1));
        when(involvementRepository.save(any())).thenReturn(new Employment());

        // when
        involvementService.updateMembership(1, membershipDTO);

        // then
        assertEquals(membership.getContributionDescription().size(), 1);
        verify(involvementRepository, times(1)).save(membership);
    }

    @Test
    public void shouldUpdateEmploymentWithValidData() {
        // given
        var mc1 = new MultiLingualContent(null, "aaa", 1);
        var employment = new Employment();
        employment.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        employment.setRole(new HashSet<>(Set.of(mc1)));
        employment.setPersonInvolved(new Person());
        var employmentDTO = new EmploymentDTO();
        employmentDTO.setEmploymentPosition(EmploymentPosition.ASSISTANT);
        var mc2 = new MultilingualContentDTO(1, "EN", "bbb", 1);
        employmentDTO.setAffiliationStatement(List.of(mc2));
        employmentDTO.setRole(List.of(mc2));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(employment));
        when(involvementRepository.save(any())).thenReturn(employment);

        // when
        involvementService.updateEmployment(1, employmentDTO);

        // then
        assertEquals(employment.getEmploymentPosition(), EmploymentPosition.ASSISTANT);
        verify(involvementRepository, times(1)).save(employment);
    }


    @Test
    public void shouldDeleteInvolvementWhenInvolvementExists() {
        // given
        var involvement = new Involvement();
        var person = new Person();
        person.addInvolvement(involvement);

        when(involvementRepository.findById(1)).thenReturn(Optional.of(involvement));

        // when
        involvementService.deleteInvolvement(1);

        //then
        assertTrue(true);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenDeletingNonExistentInvolvement() {
        // given
        when(involvementRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> involvementService.deleteInvolvement(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldAddInvolvementProofWhenInvolvementExists() {
        // given
        var involvement = new Involvement();
        involvement.setPersonInvolved(new Person());

        when(involvementRepository.findById(1)).thenReturn(Optional.of(involvement));
        when(documentFileService.saveNewPersonalDocument(any(), eq(false),
            eq(involvement.getPersonInvolved()))).thenReturn(new DocumentFile());

        // when
        involvementService.addInvolvementProof(new DocumentFileDTO(), 1);

        //then
        verify(involvementRepository, times(1)).save(involvement);
    }

    @Test
    public void shouldDeleteProofWhenDocumentExists() {
        // given
        var df = new DocumentFile();
        df.setServerFilename("UUID");
        var involvement = new Involvement();
        involvement.setProofs(new HashSet<>(Set.of(df)));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(involvement));
        when(documentFileService.findDocumentFileById(1)).thenReturn(df);

        // when
        involvementService.deleteProof(1, 1);

        // then
        verify(involvementRepository, times(1)).save(involvement);
        verify(documentFileService, times(1)).deleteDocumentFile(df.getServerFilename());
    }

    @Test
    public void shouldGetEmploymentsWhenValidPersonProvided() {
        // given
        var personId = 1;

        var organisationUnit1 = new OrganisationUnit();
        organisationUnit1.setId(100);
        organisationUnit1.setName(new HashSet<>());

        var organisationUnit2 = new OrganisationUnit();
        organisationUnit2.setId(200);
        organisationUnit2.setName(new HashSet<>());

        var employment1 = new Employment();
        employment1.setOrganisationUnit(organisationUnit1);

        var employment2 = new Employment();
        employment2.setOrganisationUnit(organisationUnit2);

        when(employmentRepository.findByPersonInvolvedId(personId))
            .thenReturn(List.of(employment1, employment2));
        when(organisationUnitService.getSuperOUsHierarchyRecursive(100))
            .thenReturn(List.of(1001, 1002));
        when(organisationUnitService.getSuperOUsHierarchyRecursive(200))
            .thenReturn(List.of(2001));

        var superOu1 = new OrganisationUnit();
        superOu1.setId(1001);
        superOu1.setName(new HashSet<>());
        var superOu2 = new OrganisationUnit();
        superOu2.setId(1002);
        superOu2.setName(new HashSet<>());
        var superOu3 = new OrganisationUnit();
        superOu3.setId(2001);
        superOu3.setName(new HashSet<>());

        when(organisationUnitService.findOne(1001)).thenReturn(superOu1);
        when(organisationUnitService.findOne(1002)).thenReturn(superOu2);
        when(organisationUnitService.findOne(2001)).thenReturn(superOu3);

        // when
        var result = involvementService.getDirectAndIndirectEmploymentsForPerson(personId);

        // then
        assertEquals(5, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getOrganisationUnitId().equals(1001)));
        assertTrue(result.stream().anyMatch(dto -> dto.getOrganisationUnitId().equals(2001)));
        assertTrue(result.stream().anyMatch(dto -> dto.getOrganisationUnitId().equals(100)));
    }

    @Test
    public void shouldEndEmploymentWhenActiveEmploymentExists() {
        // given
        Integer institutionId = 1;
        Integer personId = 1;

        var employment = new Employment();
        employment.setOrganisationUnit(new OrganisationUnit());
        employment.setPersonInvolved(new Person());
        employment.setDateTo(null); // Active employment

        when(involvementRepository.findActiveEmploymentForPersonAndInstitution(institutionId,
            personId))
            .thenReturn(Optional.of(employment));

        // when
        involvementService.endEmployment(institutionId, personId);

        // then
        assertNotNull(employment.getDateTo());
        assertEquals(LocalDate.now(), employment.getDateTo());
        verify(employmentRepository).save(employment);
        verify(personService).indexPerson(employment.getPersonInvolved());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenActiveEmploymentDoesNotExist() {
        // given
        Integer institutionId = 1;
        Integer personId = 1;

        when(involvementRepository.findActiveEmploymentForPersonAndInstitution(institutionId,
            personId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class,
            () -> involvementService.endEmployment(institutionId, personId));
        verify(employmentRepository, never()).save(any(Employment.class));
        verify(personService, never()).indexPerson(any());
    }

    @Test
    void shouldReturnEmploymentTitleWhenCurrentEmploymentExists() {
        // Given
        Integer personId = 42;
        Employment currentEmployment = new Employment();
        currentEmployment.setDateTo(null);
        currentEmployment.setEmploymentPosition(EmploymentPosition.FULL_PROFESSOR);

        when(employmentRepository.findByPersonInvolvedId(personId))
            .thenReturn(List.of(currentEmployment));

        // When
        var result = involvementService.getCurrentEmploymentTitle(personId);

        // Then
        assertThat(result).isEqualTo(EmploymentTitle.FULL_PROFESSOR);
        verify(employmentRepository).findByPersonInvolvedId(personId);
    }

    @Test
    void shouldReturnNullWhenNoCurrentEmploymentExists() {
        // Given
        Integer personId = 99;
        Employment oldEmployment = new Employment();
        oldEmployment.setDateTo(LocalDate.now()); // Already ended

        when(employmentRepository.findByPersonInvolvedId(personId))
            .thenReturn(List.of(oldEmployment));

        // When
        var result = involvementService.getCurrentEmploymentTitle(personId);

        // Then
        assertThat(result).isNull();
        verify(employmentRepository).findByPersonInvolvedId(personId);
    }

    @Test
    void shouldCreateNewEmploymentWhenNoneExists() {
        // Given
        var request = new EmploymentMigrationDTO(
            101, // personOldId
            202, // chairOldId
            EmploymentPosition.ASSOCIATE_PROFESSOR,
            LocalDate.of(2020, 1, 1),
            "acc123",
            "chairAcc456"
        );

        var person = new Person();
        person.setId(1);
        person.setAccountingIds(new HashSet<>());
        when(personService.findPersonByOldId(101)).thenReturn(person);

        var unit = new OrganisationUnit();
        unit.setId(2);
        unit.setAccountingIds(new HashSet<>());
        when(organisationUnitService.findOrganisationUnitByOldId(202)).thenReturn(unit);

        when(employmentRepository.save(any(Employment.class))).thenAnswer(
            inv -> inv.getArgument(0));

        // When
        var result = involvementService.migrateEmployment(request);

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2020, 1, 1), result.getDateFrom());
        assertEquals(EmploymentPosition.ASSOCIATE_PROFESSOR, result.getEmploymentPosition());
        assertEquals(InvolvementType.EMPLOYED_AT, result.getInvolvementType());

        verify(personService).save(person);
        verify(organisationUnitService).save(unit);
        verify(employmentRepository).save(any(Employment.class));
        verify(personService).indexPerson(person);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(person.getId());
    }

    @Test
    void shouldUpdateExistingEmploymentWhenMatchingOneExists() {
        // Given
        var request = new EmploymentMigrationDTO(
            null, null,
            EmploymentPosition.TEACHING_ASSISTANT,
            LocalDate.of(2021, 5, 15),
            "acc123",
            "chairAcc456"
        );

        var unit = new OrganisationUnit();
        unit.setId(2);
        when(organisationUnitService.findOrganisationUnitByAccountingId("chairAcc456")).thenReturn(
            unit);

        var employment = new Employment();
        employment.setEmploymentPosition(EmploymentPosition.TEACHING_ASSISTANT);
        employment.setOrganisationUnit(unit);
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);

        var person = new Person();
        person.setId(1);
        person.setAccountingIds(new HashSet<>());
        person.setInvolvements(Set.of(employment));
        when(personService.findPersonByAccountingId("acc123")).thenReturn(person);

        when(involvementRepository.save(any(Employment.class))).thenAnswer(
            inv -> inv.getArgument(0));

        // When
        var result = involvementService.migrateEmployment(request);

        // Then
        assertEquals(LocalDate.of(2021, 5, 15), result.getDateFrom());
        verify(involvementRepository).save(employment);
        verify(personService).save(person);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(person.getId());
    }

    @Test
    void shouldCloseOtherOpenEmployments() {
        // Given
        var request = new EmploymentMigrationDTO(
            null, null,
            EmploymentPosition.TEACHING_ASSISTANT,
            LocalDate.of(2022, 6, 1),
            "acc1",
            "accChair"
        );

        var unit = new OrganisationUnit();
        unit.setId(33);
        when(organisationUnitService.findOrganisationUnitByAccountingId("accChair")).thenReturn(
            unit);

        var person = new Person();
        person.setId(10);
        person.setAccountingIds(new HashSet<>());

        var oldEmployment = new Employment();
        oldEmployment.setEmploymentPosition(EmploymentPosition.ASSISTANT_PROFESSOR);
        oldEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        oldEmployment.setOrganisationUnit(unit);
        oldEmployment.setDateFrom(LocalDate.of(2020, 1, 1));
        oldEmployment.setDateTo(null);

        person.setInvolvements(new HashSet<>(List.of(oldEmployment)));
        when(personService.findPersonByAccountingId("acc1")).thenReturn(person);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(
            inv -> inv.getArgument(0));

        // When
        var result = involvementService.migrateEmployment(request);

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2022, 6, 1), oldEmployment.getDateTo());
        verify(personService).save(person);
    }

    @Test
    void shouldMigrateInternalIdentifierWhenOldIdMatches() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person = new Person();
        person.setOldIds(Set.of(100, 200));
        person.setInternalIdentifiers(new HashSet<>(Set.of("500")));

        var employment = new Employment();
        employment.setPersonInvolved(person);
        employment.setDateTo(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertTrue(person.getInternalIdentifiers().contains("1000"));
        assertNull(employment.getDateTo());

        verify(personService).save(person);
        verify(involvementRepository).save(employment);
    }

    @Test
    void shouldSetEndDateWhenNoOldIdMatches() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(300, 3000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person = new Person();
        person.setOldIds(Set.of(100, 200));
        person.setInternalIdentifiers(new HashSet<>(Set.of("500")));

        var employment = new Employment();
        employment.setPersonInvolved(person);
        employment.setDateTo(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertEquals(Set.of("500"), person.getInternalIdentifiers());
        assertEquals(defaultEndDate, employment.getDateTo());

        verify(personService, never()).save(person);
        verify(involvementRepository).save(employment);
    }

    @Test
    void shouldSkipEmploymentWhenPersonInvolvedIsNull() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var employment = new Employment();
        employment.setPersonInvolved(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        verify(personService, never()).save(any());
        verify(involvementRepository, never()).save(any());
    }

    @Test
    void shouldHandleMultipleEmploymentsWithMixedResults() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000, 300, 3000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person1 = new Person();
        person1.setOldIds(Set.of(100, 200));
        person1.setInternalIdentifiers(new HashSet<>(Set.of("500")));
        var employment1 = new Employment();
        employment1.setPersonInvolved(person1);
        employment1.setDateTo(null);

        var person2 = new Person();
        person2.setOldIds(Set.of(400, 500));
        person2.setInternalIdentifiers(new HashSet<>(Set.of("600")));
        var employment2 = new Employment();
        employment2.setPersonInvolved(person2);
        employment2.setDateTo(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment1, employment2));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertTrue(person1.getInternalIdentifiers().contains("1000"));
        assertNull(employment1.getDateTo());

        assertEquals(Set.of("600"), person2.getInternalIdentifiers());
        assertEquals(defaultEndDate, employment2.getDateTo());

        verify(involvementRepository, times(2)).save(any(Employment.class));
    }

    @Test
    void shouldAddMultipleInternalIdentifiersWhenMultipleOldIdsMatch() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000, 200, 2000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person = new Person();
        person.setOldIds(Set.of(100, 200, 300));
        person.setInternalIdentifiers(new HashSet<>(Set.of("500")));

        var employment = new Employment();
        employment.setPersonInvolved(person);
        employment.setDateTo(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertTrue(person.getInternalIdentifiers().contains("1000"));
        assertTrue(person.getInternalIdentifiers().contains("2000"));
        assertFalse(person.getInternalIdentifiers().contains("3000"));
        assertEquals(3, person.getInternalIdentifiers().size());

        verify(personService, atLeastOnce()).save(person);
        verify(involvementRepository).save(employment);
    }

    @Test
    void shouldDoNothingWhenInstitutionHierarchyIsEmpty() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(Collections.emptyList());
        when(involvementRepository.findActiveEmploymentsForInstitutions(Collections.emptyList()))
            .thenReturn(Collections.emptyList());

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        verify(involvementRepository, never()).save(any());
        verify(personService, never()).save(any());
    }

    @Test
    void shouldSetEndDateWhenPersonHasEmptyOldIds() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person = new Person();
        person.setOldIds(Collections.emptySet());
        person.setInternalIdentifiers(new HashSet<>(Set.of("500")));

        var employment = new Employment();
        employment.setPersonInvolved(person);
        employment.setDateTo(null);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertEquals(Set.of("500"), person.getInternalIdentifiers());
        assertEquals(defaultEndDate, employment.getDateTo());

        verify(personService, never()).save(person);
        verify(involvementRepository).save(employment);
    }

    @Test
    void shouldPreserveExistingEndDateWhenMigrationOccurs() {
        // Given
        var institutionId = 1;
        var oldToInternalMapping = Map.of(100, 1000);
        var existingEndDate = LocalDate.of(2022, 6, 30);
        var defaultEndDate = LocalDate.of(2023, 12, 31);
        var dto = new InternalIdentifierMigrationDTO(oldToInternalMapping, institutionId,
            defaultEndDate, false);

        var institutionIds = List.of(1, 2, 3);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(institutionIds);

        var person = new Person();
        person.setOldIds(Set.of(100));
        person.setInternalIdentifiers(new HashSet<>(Set.of("500")));

        var employment = new Employment();
        employment.setPersonInvolved(person);
        employment.setDateTo(existingEndDate);

        when(involvementRepository.findActiveEmploymentsForInstitutions(institutionIds))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(dto);

        // Then
        assertTrue(person.getInternalIdentifiers().contains("1000"));
        assertEquals(existingEndDate, employment.getDateTo());

        verify(personService).save(person);
        verify(involvementRepository).save(employment);
    }

    @Test
    void shouldCreateNewEmploymentWhenPersonAndOrganisationUnitExist() {
        // Given
        var migrationDTO = List.of(new ExtraEmploymentMigrationDTO(
            12345, EmploymentPosition.FULL_PROFESSOR, LocalDate.of(2020, 1, 1),
            "University of Belgrade", new PersonNameDTO(null, "John", "", "Doe", null, null)
        ));

        var organisationUnit = new OrganisationUnitIndex();
        organisationUnit.setDatabaseId(1);

        var result = new OrganisationUnitIndex();
        result.setNameSr("University of Belgrade");
        result.setDatabaseId(1);
        var searchResults = List.of(result);

        when(organisationUnitService.searchOrganisationUnits(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(searchResults));

        when(personService.findPersonByAccountingId("12345")).thenReturn(null);

        var newPerson = new Person();
        newPerson.setId(100);
        newPerson.setInvolvements(new HashSet<>());

        when(personService.createPersonWithBasicInfo(any(), eq(true))).thenReturn(newPerson);
        when(employmentRepository.save(any())).thenReturn(new Employment());
        when(organisationUnitService.findOne(1)).thenReturn(new OrganisationUnit() {{
            setId(1);
        }});
        when(personService.save(any())).thenReturn(newPerson);

        // When
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1));

        // Then
        verify(personService).createPersonWithBasicInfo(argThat(dto ->
            dto.getPersonName().getFirstname().equals("John") &&
                dto.getPersonName().getLastname().equals("Doe") &&
                dto.getOrganisationUnitId().equals(1) &&
                dto.getEmploymentPosition().equals(EmploymentPosition.FULL_PROFESSOR)
        ), eq(true));

        verify(employmentRepository).save(argThat(employment ->
            employment.getPersonInvolved().equals(newPerson) &&
                employment.getEmploymentPosition().equals(EmploymentPosition.FULL_PROFESSOR) &&
                employment.getDateFrom().equals(LocalDate.of(2020, 1, 1))
        ));

        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(100);
    }

    @Test
    void shouldUpdateExistingEmploymentWhenEmploymentAlreadyExists() {
        // Given
        var migrationDTO = List.of(new ExtraEmploymentMigrationDTO(
            12345, EmploymentPosition.FULL_PROFESSOR, LocalDate.of(2020, 1, 1),
            "University of Belgrade", new PersonNameDTO(null, "John", "", "Doe", null, null)
        ));

        var result = new OrganisationUnitIndex();
        result.setNameSr("University of Belgrade");
        result.setDatabaseId(1);
        var searchResults = List.of(result);
        when(organisationUnitService.searchOrganisationUnits(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(searchResults));

        var existingPerson = new Person();
        existingPerson.setId(100);
        existingPerson.setInvolvements(new HashSet<>(Set.of(new Employment() {{
            setInvolvementType(InvolvementType.EMPLOYED_AT);
        }})));

        var existingEmployment = new Employment();
        existingEmployment.setId(500);
        existingEmployment.setEmploymentPosition(EmploymentPosition.FULL_PROFESSOR);
        existingEmployment.setDateFrom(LocalDate.of(2019, 1, 1));
        existingEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);

        existingEmployment.setOrganisationUnit(organisationUnit);
        existingPerson.setInvolvements(new HashSet<>(Set.of(existingEmployment)));

        when(personService.findPersonByAccountingId("12345")).thenReturn(existingPerson);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);
        when(employmentRepository.save(any())).thenReturn(new Employment() {{
            setInvolvementType(InvolvementType.EMPLOYED_AT);
        }});
        when(personService.save(any())).thenReturn(existingPerson);

        // When
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1));

        // Then
        verify(employmentRepository, never()).save(any());
        verify(personService, never()).createPersonWithBasicInfo(any(), anyBoolean());
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(100);
    }

    @Test
    void shouldSkipMigrationWhenOrganisationUnitNotFound() {
        // Given
        var migrationDTO = List.of(new ExtraEmploymentMigrationDTO(
            12345, EmploymentPosition.FULL_PROFESSOR, LocalDate.of(2020, 1, 1),
            "Non Existent University", new PersonNameDTO(null, "John", "", "Doe", null, null)
        ));

        when(organisationUnitService.searchOrganisationUnits(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        // When
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1));

        // Then
        verify(personService, never()).findPersonByAccountingId(any());
        verify(personService, never()).createPersonWithBasicInfo(any(), anyBoolean());
        verify(employmentRepository, never()).save(any());
        verify(userService, never()).updateResearcherCurrentOrganisationUnitIfBound(any());
    }

    @Test
    void shouldProcessMultipleMigrationDTOs() {
        // Given
        var migrationDTOs = List.of(
            new ExtraEmploymentMigrationDTO(12345, EmploymentPosition.FULL_PROFESSOR,
                LocalDate.now(),
                "University A", new PersonNameDTO(null, "John", "", "Doe", null, null)),
            new ExtraEmploymentMigrationDTO(67890, EmploymentPosition.RESEARCHER, LocalDate.now(),
                "University B", new PersonNameDTO(null, "Jane", "", "Smith", null, null))
        );

        var result = new OrganisationUnitIndex();
        result.setNameSr("University A");
        result.setDatabaseId(1);
        var searchResultsA = List.of(result);
        var result2 = new OrganisationUnitIndex();
        result2.setNameSr("University B");
        result2.setDatabaseId(1);
        var searchResultsB = List.of(result2);

        when(organisationUnitService.searchOrganisationUnits(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(searchResultsA))
            .thenReturn(new PageImpl<>(searchResultsB));

        var person1 = new Person();
        person1.setId(100);
        person1.setInvolvements(new HashSet<>());
        var person2 = new Person();
        person2.setId(200);
        person2.setInvolvements(new HashSet<>());

        when(personService.findPersonByAccountingId("12345")).thenReturn(person1);
        when(personService.findPersonByAccountingId("67890")).thenReturn(person2);

        var organisationUnit1 = new OrganisationUnit();
        organisationUnit1.setId(1);
        var organisationUnit2 = new OrganisationUnit();
        organisationUnit2.setId(2);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit1);
        when(organisationUnitService.findOne(2)).thenReturn(organisationUnit2);
        when(employmentRepository.save(any())).thenReturn(new Employment());
        when(personService.save(any())).thenReturn(person1);

        // When
        involvementService.migrateEmployment(migrationDTOs, LocalDate.of(2024, 11, 1));

        // Then
        verify(personService, times(2)).save(any(Person.class));
        verify(userService, times(2)).updateResearcherCurrentOrganisationUnitIfBound(any());
        verify(employmentRepository, times(2)).save(any(Employment.class));
    }

    @Test
    void shouldFindExistingEmploymentByPositionAndOrganisationUnit() {
        // Given
        var migrationDTO = List.of(new ExtraEmploymentMigrationDTO(
            12345, EmploymentPosition.FULL_PROFESSOR, LocalDate.of(2020, 1, 1),
            "University of Belgrade", new PersonNameDTO(null, "John", "", "Doe", null, null)
        ));

        var result = new OrganisationUnitIndex();
        result.setNameSr("University of Belgrade");
        result.setDatabaseId(1);
        var searchResults = List.of(result);
        when(organisationUnitService.searchOrganisationUnits(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(searchResults));

        var existingPerson = new Person();
        existingPerson.setId(100);

        var existingEmployment = new Employment();
        existingEmployment.setId(500);
        existingEmployment.setEmploymentPosition(EmploymentPosition.FULL_PROFESSOR);
        existingEmployment.setDateFrom(LocalDate.of(2019, 1, 1));

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);

        existingEmployment.setOrganisationUnit(organisationUnit);
        existingEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        existingPerson.setInvolvements(new HashSet<>(Set.of(existingEmployment)));

        when(personService.findPersonByAccountingId("12345")).thenReturn(existingPerson);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);
        when(personService.save(any())).thenReturn(existingPerson);

        // When
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1));

        // Then
        verify(employmentRepository, never()).save(any());
    }
}
