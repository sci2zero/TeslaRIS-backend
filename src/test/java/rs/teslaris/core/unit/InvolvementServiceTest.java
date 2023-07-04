package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.PersonService;
import rs.teslaris.core.service.impl.InvolvementServiceImpl;

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

    @InjectMocks
    private InvolvementServiceImpl involvementService;

    @Test
    public void shouldReturnInvolvementWhenInvolvementExists() {
        // given
        var expected = new Involvement();

        when(involvementRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var actual = involvementService.findInvolvementById(1);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenInvolvementDoesNotExist() {
        // given
        when(involvementRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> involvementService.findInvolvementById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReturnGenericInvolvementWhenInvolvementExists() {
        // given
        var mc1 = new MultiLingualContent(new LanguageTag(), "aaa", 1);
        var education = new Education();
        education.setOrganisationUnit(new OrganisationUnit());
        education.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        education.setProofs(new HashSet<>());
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
        var mc = new MultilingualContentDTO(1, "aaa", 1);
        educationDTO.setAffiliationStatement(List.of(mc));
        educationDTO.setThesisTitle(List.of(mc));
        educationDTO.setTitle(List.of(mc));
        educationDTO.setAbbreviationTitle(List.of(mc));

        when(personService.findPersonById(1)).thenReturn(person);
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
        var mc = new MultilingualContentDTO(1, "aaa", 1);
        membershipDTO.setAffiliationStatement(List.of(mc));
        membershipDTO.setRole(List.of(mc));
        membershipDTO.setContributionDescription(List.of(mc));

        when(personService.findPersonById(1)).thenReturn(person);
        when(involvementRepository.save(any())).thenReturn(new Membership());

        // when
        var result = involvementService.addMembership(1, membershipDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(result);
    }

    @Test
    public void shouldAddEmploymentWithValidData() {
        // given
        var person = new Person();
        var employmentDTO = new EmploymentDTO();
        var mc = new MultilingualContentDTO(1, "aaa", 1);
        employmentDTO.setAffiliationStatement(List.of(mc));
        employmentDTO.setRole(List.of(mc));

        when(personService.findPersonById(1)).thenReturn(person);
        when(involvementRepository.save(any())).thenReturn(new Employment());

        // when
        var result = involvementService.addEmployment(1, employmentDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(result);
    }

    @Test
    public void shouldUpdateEducationWithValidData() {
        // given
        var mc1 = new MultiLingualContent(null, "aaa", 1);
        var education = new Education();
        education.setAffiliationStatement(new HashSet<>(Set.of(mc1)));
        education.setProofs(new HashSet<>());
        education.setThesisTitle(new HashSet<>(Set.of(mc1)));
        education.setTitle(new HashSet<>(Set.of(mc1)));
        education.setAbbreviationTitle(new HashSet<>(Set.of(mc1)));
        var educationDTO = new EducationDTO();
        var mc2 = new MultilingualContentDTO(1, "bbb", 1);
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
        membership.setProofs(new HashSet<>());
        membership.setContributionDescription(new HashSet<>(Set.of(mc1)));
        membership.setRole(new HashSet<>(Set.of(mc1)));
        var membershipDTO = new MembershipDTO();
        var mc2 = new MultilingualContentDTO(1, "bbb", 1);
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
        employment.setProofs(new HashSet<>());
        employment.setRole(new HashSet<>(Set.of(mc1)));
        var employmentDTO = new EmploymentDTO();
        employmentDTO.setEmploymentPosition(EmploymentPosition.ASSISTANT);
        var mc2 = new MultilingualContentDTO(1, "bbb", 1);
        employmentDTO.setAffiliationStatement(List.of(mc2));
        employmentDTO.setRole(List.of(mc2));

        when(involvementRepository.findById(1)).thenReturn(Optional.of(employment));
        when(involvementRepository.save(any())).thenReturn(new Employment());

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
        involvement.setProofs(new HashSet<>());
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
        involvement.setProofs(new HashSet<>());

        when(involvementRepository.findById(1)).thenReturn(Optional.of(involvement));
        when(documentFileService.saveNewDocument(any(), eq(true))).thenReturn(new DocumentFile());

        // when
        involvementService.addInvolvementProofs(
            List.of(new DocumentFileDTO(), new DocumentFileDTO()), 1);

        //then
        verify(involvementRepository, times(2)).save(involvement);
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

        //then
        verify(involvementRepository, times(1)).save(involvement);
        verify(documentFileService, times(1)).deleteDocumentFile(df.getServerFilename());
    }

}
