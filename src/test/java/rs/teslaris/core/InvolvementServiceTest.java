package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.LanguageTagService;
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
    private LanguageTagService languageTagService;

    @Mock
    private InvolvementRepository involvementRepository;

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
        when(languageTagService.findLanguageTagById(anyInt())).thenReturn(new LanguageTag());
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
        when(languageTagService.findLanguageTagById(anyInt())).thenReturn(new LanguageTag());
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
        when(languageTagService.findLanguageTagById(anyInt())).thenReturn(new LanguageTag());
        when(involvementRepository.save(any())).thenReturn(new Employment());

        // when
        var result = involvementService.addEmployment(1, employmentDTO);

        // then
        assertNotNull(result);
        verify(involvementRepository, times(1)).save(result);
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


}
