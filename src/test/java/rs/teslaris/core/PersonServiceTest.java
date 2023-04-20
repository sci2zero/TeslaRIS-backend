package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.impl.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.PersonServiceImpl;

@SpringBootTest
public class PersonServiceTest {

    @Mock
    OrganisationUnitServiceImpl organisationUnitService;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonServiceImpl personService;

    @Test
    public void shouldReturnPersonWhenPersonExists() {
        // given
        var expectedPerson = new Person();

        when(personRepository.findById(1)).thenReturn(Optional.of(expectedPerson));

        // when
        Person actualPerson = personService.findPersonById(1);

        // then
        assertEquals(expectedPerson, actualPerson);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonDoesNotExist() {
        // given
        when(personRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> personService.findPersonById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    void testCreatePersonWithBasicInfo() {
        // given
        BasicPersonDTO personDTO = new BasicPersonDTO();
        PersonNameDTO personNameDTO = new PersonNameDTO();
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

        // when
        OrganisationUnit employmentInstitution = new OrganisationUnit();
        when(organisationUnitService.findOrganisationalUnitById(2)).thenReturn(
            employmentInstitution);
        when(personRepository.save(any(Person.class))).thenReturn(new Person());

        // then
        Person result = personService.createPersonWithBasicInfo(personDTO);
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
        Involvement currentEmployment = result.getInvolvements().iterator().next();
        assertEquals(ApproveStatus.APPROVED, currentEmployment.getApproveStatus());
        assertEquals(InvolvementType.EMPLOYED_AT, currentEmployment.getInvolvementType());
        assertEquals(EmploymentPosition.ASSISTANT_PROFESSOR,
            ((Employment) currentEmployment).getEmploymentPosition());
    }

}
