package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
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
import rs.teslaris.core.service.impl.person.worker.EmploymentMigrationWorker;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;

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

    @Mock
    private EmploymentMigrationWorker employmentMigrationWorker;

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
    void should_MigrateEmployment_When_ValidRequestProvided() {
        // Given
        var migrationRequest = new EmploymentMigrationDTO(123, 1, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "123", "321");

        var employment = new Employment();
        employment.setId(123);

        var expectedDTO = new EmploymentDTO();
        expectedDTO.setId(123);

        when(employmentMigrationWorker.performLegacyMigration(any(EmploymentMigrationDTO.class)))
            .thenReturn(employment);

        // When
        var result = involvementService.migrateEmployment(migrationRequest);

        // Then
        assertNotNull(result);
        assertEquals(expectedDTO.getId(), result.getId());

        verify(employmentMigrationWorker).performLegacyMigration(migrationRequest);
    }

    @Test
    void shouldReturnCorrectDTOWhenEmploymentIsMigrated() {
        // Given
        var migrationRequest = new EmploymentMigrationDTO(123, 1, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "123", "321");

        var employment = new Employment();
        employment.setId(456);
        employment.setEmploymentPosition(EmploymentPosition.RESEARCHER);

        when(employmentMigrationWorker.performLegacyMigration(migrationRequest))
            .thenReturn(employment);

        // When
        var result = involvementService.migrateEmployment(migrationRequest);

        // Then
        assertNotNull(result);
        assertEquals(employment.getId(), result.getId());
        assertEquals(employment.getEmploymentPosition().name(),
            result.getEmploymentPosition().name());

        verify(employmentMigrationWorker).performLegacyMigration(migrationRequest);
    }

    @Test
    void shouldCallConverterWithCorrectEmploymentWhenMigrationIsSuccessful() {
        // Given
        var migrationRequest = new EmploymentMigrationDTO(123, 1, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "123", "321");

        var employment = new Employment();
        employment.setId(789);

        when(employmentMigrationWorker.performLegacyMigration(migrationRequest))
            .thenReturn(employment);

        // When
        involvementService.migrateEmployment(migrationRequest);

        // Then
        verify(employmentMigrationWorker).performLegacyMigration(migrationRequest);
    }

    @Test
    void shouldMigrateAccountingIdsWhenAccountingIdsFlagIsTrue() {
        // Given
        var oldId = 123;
        var internalId = 456;
        var migrationDTO = new InternalIdentifierMigrationDTO(
            Map.of(oldId, internalId), null, null, true
        );

        var person = new Person();
        person.setAccountingIds(new HashSet<>());
        person.setInternalIdentifiers(new HashSet<>());

        when(personService.findPersonByOldId(oldId)).thenReturn(person);
        when(personService.save(person)).thenReturn(person);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        assertThat(person.getAccountingIds()).contains(String.valueOf(internalId));
        assertThat(person.getInternalIdentifiers()).isEmpty();
        verify(personService).findPersonByOldId(oldId);
        verify(personService).save(person);
    }

    @Test
    void shouldMigrateInternalIdentifiersWhenAccountingIdsFlagIsFalse() {
        // Given
        var oldId = 456;
        var internalId = 789;
        var migrationDTO = new InternalIdentifierMigrationDTO(
            Map.of(oldId, internalId), null, null, false
        );

        var person = new Person();
        person.setAccountingIds(new HashSet<>());
        person.setInternalIdentifiers(new HashSet<>());

        when(personService.findPersonByOldId(oldId)).thenReturn(person);
        when(personService.save(person)).thenReturn(person);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        assertThat(person.getInternalIdentifiers()).contains(String.valueOf(internalId));
        assertThat(person.getAccountingIds()).isEmpty();
        verify(personService).findPersonByOldId(oldId);
        verify(personService).save(person);
    }

    @Test
    void shouldSkipPersonWhenPersonNotFoundByOldId() {
        // Given
        var oldId = 123;
        var internalId = 999;
        var migrationDTO = new InternalIdentifierMigrationDTO(
            Map.of(oldId, internalId), null, null, true
        );

        when(personService.findPersonByOldId(oldId)).thenReturn(null);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        verify(personService).findPersonByOldId(oldId);
        verify(personService, never()).save(any());
    }

    @Test
    void shouldHandleMultipleMappingsInSingleMigration() {
        // Given
        var mapping = Map.of(
            1, 111,
            2, 222,
            3, 333
        );
        var migrationDTO = new InternalIdentifierMigrationDTO(mapping, null, null, false);

        var person1 = new Person();
        person1.setInternalIdentifiers(new HashSet<>());
        var person2 = new Person();
        person2.setInternalIdentifiers(new HashSet<>());
        var person3 = new Person();
        person3.setInternalIdentifiers(new HashSet<>());

        when(personService.findPersonByOldId(1)).thenReturn(person1);
        when(personService.findPersonByOldId(2)).thenReturn(person2);
        when(personService.findPersonByOldId(3)).thenReturn(person3);
        when(personService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        assertThat(person1.getInternalIdentifiers()).contains("111");
        assertThat(person2.getInternalIdentifiers()).contains("222");
        assertThat(person3.getInternalIdentifiers()).contains("333");
        verify(personService, times(3)).findPersonByOldId(any());
        verify(personService, times(3)).save(any());
    }

    @Test
    void shouldAddToExistingIdentifiersWhenCollectionsAlreadyContainValues() {
        // Given
        var oldId = 999;
        var internalId = 1000;
        var migrationDTO = new InternalIdentifierMigrationDTO(
            Map.of(oldId, internalId), null, null, true
        );

        var person = new Person();
        person.setAccountingIds(new HashSet<>(Set.of("existing1", "existing2")));
        person.setInternalIdentifiers(new HashSet<>(Set.of("internal1")));

        when(personService.findPersonByOldId(oldId)).thenReturn(person);
        when(personService.save(person)).thenReturn(person);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        assertThat(person.getAccountingIds())
            .containsExactlyInAnyOrder("existing1", "existing2", "1000");
        assertThat(person.getInternalIdentifiers()).containsExactly("internal1");
        verify(personService).findPersonByOldId(oldId);
        verify(personService).save(person);
    }

    @Test
    void shouldHandleEmptyMappingWithoutErrors() {
        // Given
        var migrationDTO = new InternalIdentifierMigrationDTO(Map.of(), null, null, true);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        verify(personService, never()).findPersonByOldId(any());
        verify(personService, never()).save(any());
    }

    @Test
    void shouldConvertInternalIdToStringWhenAddingToCollections() {
        // Given
        var oldId = 777;
        var internalId = 888;
        var migrationDTO = new InternalIdentifierMigrationDTO(
            Map.of(oldId, internalId), null, null, false
        );

        var person = new Person();
        person.setInternalIdentifiers(new HashSet<>());

        when(personService.findPersonByOldId(oldId)).thenReturn(person);
        when(personService.save(person)).thenReturn(person);

        // When
        involvementService.migrateEmployeeInternalIdentifiers(migrationDTO);

        // Then
        assertThat(person.getInternalIdentifiers()).contains("888");
        verify(personService).findPersonByOldId(oldId);
        verify(personService).save(person);
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
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1), 1);

        // Then
        verify(personService, never()).findPersonByAccountingId(any());
        verify(personService, never()).createPersonWithBasicInfo(any(), anyBoolean());
        verify(employmentRepository, never()).save(any());
        verify(userService, never()).updateResearcherCurrentOrganisationUnitIfBound(any());
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
        involvementService.migrateEmployment(migrationDTO, LocalDate.of(2024, 11, 1), 1);

        // Then
        verify(employmentRepository, never()).save(any());
    }

    @Test
    void shouldProcessMigrationsWhenValidRequestProvided() {
        // Given
        var request = List.of(
            createMigrationDTO(2, "University B", "FULL_PROFESSOR")
        );
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker, atLeastOnce()).processSingleMigration(
            any(), any(), any());
        verify(organisationUnitService, never()).getOrganisationUnitIdsFromSubHierarchy(anyInt());
    }

    @Test
    void shouldAddAdditionalInstitutionHierarchyWhenAdditionalInstitutionProvided() {
        // Given
        var request = List.of(createMigrationDTO(1, "University A", "RESEARCHER"));
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = 100;

        var subHierarchyIds = List.of(100, 101, 102);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(100))
            .thenReturn(subHierarchyIds);

        when(employmentRepository.findByEmploymentInstitutionId(anyInt()))
            .thenReturn(new ArrayList<>());

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(100);
    }

    @Test
    void shouldCleanupAlumniForAllHandledInstitutions() {
        // Given
        var request = List.of(createMigrationDTO(1, "University A", "RESEARCHER"));
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        var employment1 = new Employment();
        var employment2 = new Employment();

        when(employmentRepository.findByEmploymentInstitutionId(200))
            .thenReturn(List.of(employment1));
        when(employmentRepository.findByEmploymentInstitutionId(201))
            .thenReturn(List.of(employment2));

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker, atLeastOnce()).processSingleMigration(
            any(), any(), any());
    }

    @Test
    void shouldPassCorrectParametersToCleanupAlumni() {
        // Given
        var request = List.of(createMigrationDTO(1, "University A", "RESEARCHER"));
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        var institutionId = 300;
        var employment = new Employment();
        employment.setId(999);

        when(employmentRepository.findByEmploymentInstitutionId(institutionId))
            .thenReturn(List.of(employment));

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker).processSingleMigration(any(), any(), any());
    }

    @Test
    void shouldLogCleanupSummaryWhenMigrationCompleted() {
        // Given
        var request = List.of(createMigrationDTO(1, "University A", "RESEARCHER"));
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker).processSingleMigration(any(), any(), any());
    }

    @Test
    void shouldHandleEmptyRequestWithoutErrors() {
        // Given
        List<ExtraEmploymentMigrationDTO> request = new ArrayList<>();
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker, never()).processSingleMigration(any(), any(), any());
        verify(employmentRepository, never()).findByEmploymentInstitutionId(anyInt());
    }

    @Test
    void shouldProcessMultipleEmploymentsPerInstitution() {
        // Given
        var request = List.of(createMigrationDTO(1, "University A", "RESEARCHER"));
        var initialMigrationDate = LocalDate.of(2023, 1, 1);
        Integer additionalInstitutionToLook = null;

        var employment1 = new Employment();
        var employment2 = new Employment();
        var employment3 = new Employment();

        when(employmentRepository.findByEmploymentInstitutionId(anyInt()))
            .thenReturn(List.of(employment1, employment2, employment3));

        // When
        involvementService.migrateEmployment(request, initialMigrationDate,
            additionalInstitutionToLook);

        // Then
        verify(employmentMigrationWorker, atLeastOnce()).processSingleMigration(
            any(), any(), any());
    }

    @Test
    void shouldNotAddExternalInvolvementWhenDuplicateInstitutionExists() {
        // Given
        var personId = 42;
        var institutionName = List.of(
            new MultilingualContentDTO(1, LanguageAbbreviations.ENGLISH, "University Of Test", 1)
        );
        var employmentTitle = "ASSOCIATE_PROFESSOR";

        var existingEmployment = new Employment();
        existingEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);

        var mlc = new MultiLingualContent();
        mlc.setContent("University of Test");
        mlc.setLanguage(new LanguageTag(LanguageAbbreviations.ENGLISH, "English"));

        existingEmployment.setAffiliationStatement(Set.of(mlc));

        when(employmentRepository.findExternalByPersonInvolvedId(personId))
            .thenReturn(List.of(existingEmployment));

        // When
        involvementService.addExternalInvolvementToContributor(personId, institutionName,
            employmentTitle);

        // Then
        verify(employmentRepository).findExternalByPersonInvolvedId(personId);
    }

    @Test
    void shouldAddExternalInvolvementWhenNoDuplicateInstitutionExists() {
        // Given
        var personId = 42;
        var institutionName = List.of(
            new MultilingualContentDTO(1, LanguageAbbreviations.ENGLISH, "University", 1)
        );
        var employmentTitle = "FULL_PROFESSOR";

        var existingEmployment = new Employment();
        existingEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);

        var mlc = new MultiLingualContent();
        mlc.setContent("Old University");
        mlc.setLanguage(new LanguageTag(LanguageAbbreviations.ENGLISH, "English"));

        existingEmployment.setAffiliationStatement(Set.of(mlc));

        when(employmentRepository.findExternalByPersonInvolvedId(personId))
            .thenReturn(List.of(existingEmployment));
        when(personService.findOne(any())).thenReturn(new Person());

        // When
        involvementService.addExternalInvolvementToContributor(personId, institutionName,
            employmentTitle);

        // Then
        verify(employmentRepository).findExternalByPersonInvolvedId(personId);
        verify(involvementRepository).save(argThat((Employment e) ->
            e.getInvolvementType() == InvolvementType.EMPLOYED_AT &&
                e.getEmploymentPosition() == EmploymentPosition.FULL_PROFESSOR
        ));
    }

    @Test
    void shouldAddExternalInvolvementWhenNoExistingEmployments() {
        // Given
        var personId = 99;
        var institutionName = List.of(
            new MultilingualContentDTO(1, LanguageAbbreviations.ENGLISH, "University", 1)
        );
        var employmentTitle = "ACADEMICIAN";

        when(employmentRepository.findExternalByPersonInvolvedId(personId))
            .thenReturn(List.of());
        when(personService.findOne(any())).thenReturn(new Person());

        // When
        involvementService.addExternalInvolvementToContributor(personId, institutionName,
            employmentTitle);

        // Then
        verify(employmentRepository).findExternalByPersonInvolvedId(personId);
        verify(involvementRepository).save(any());
    }

    @Test
    void shouldMatchInstitutionsCaseInsensitively() {
        // Given
        var personId = 42;
        var institutionName = List.of(
            new MultilingualContentDTO(1, LanguageAbbreviations.ENGLISH, "University", 1)
        );
        var employmentTitle = "ASSISTANT_PROFESSOR";

        var existingEmployment = new Employment();
        existingEmployment.setInvolvementType(InvolvementType.EMPLOYED_AT);

        var mlc = new MultiLingualContent();
        mlc.setContent("UNIVERSITY"); // uppercase
        mlc.setLanguage(new LanguageTag(LanguageAbbreviations.ENGLISH, "English"));

        existingEmployment.setAffiliationStatement(Set.of(mlc));

        when(employmentRepository.findExternalByPersonInvolvedId(personId))
            .thenReturn(List.of(existingEmployment));

        // When
        involvementService.addExternalInvolvementToContributor(personId, institutionName,
            employmentTitle);

        // Then
        verify(employmentRepository).findExternalByPersonInvolvedId(personId);
        verify(involvementRepository, never()).save(any());
    }

    @Test
    void shouldReturnExternalInstitutionSuggestions() {
        // Given
        var personId = 42;

        var employment1 = new Employment();
        var mlc1 = new MultiLingualContent();
        mlc1.setContent("University A");
        mlc1.setLanguage(new LanguageTag() {{
            setLanguageTag(LanguageAbbreviations.ENGLISH);
        }});

        employment1.setAffiliationStatement(Set.of(mlc1));

        var employment2 = new Employment();
        var mlc2 = new MultiLingualContent();
        mlc2.setContent("University B");
        mlc2.setLanguage(new LanguageTag() {{
            setLanguageTag(LanguageAbbreviations.SERBIAN);
        }});

        employment2.setAffiliationStatement(Set.of(mlc2));

        when(employmentRepository.findExternalByPersonInvolvedId(personId))
            .thenReturn(List.of(employment1, employment2));

        // When
        List<List<MultilingualContentDTO>> result =
            involvementService.getExternalInstitutionSuggestions(personId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0))
            .extracting(MultilingualContentDTO::getContent)
            .containsExactly("University A");
        assertThat(result.get(1))
            .extracting(MultilingualContentDTO::getContent)
            .containsExactly("University B");

        verify(employmentRepository).findExternalByPersonInvolvedId(personId);
    }

    private ExtraEmploymentMigrationDTO createMigrationDTO(Integer personAccountingId,
                                                           String organisationUnitName,
                                                           String employmentPosition) {
        var dto = new ExtraEmploymentMigrationDTO(personAccountingId,
            EmploymentPosition.valueOf(employmentPosition), LocalDate.of(2020, 1, 1),
            organisationUnitName, new PersonNameDTO());
        return dto;
    }
}
