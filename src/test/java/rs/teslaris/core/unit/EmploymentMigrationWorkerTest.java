package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.person.worker.EmploymentMigrationWorker;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
public class EmploymentMigrationWorkerTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private PersonService personService;

    @Mock
    private EmploymentRepository employmentRepository;

    @Mock
    private InvolvementRepository involvementRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EmploymentMigrationWorker employmentMigrationWorker;


    @Test
    void shouldPerformMigrationWhenPersonFoundByOldId() {
        // Given
        var request = new EmploymentMigrationDTO(100, 100, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "", "");
        var person = new Person();
        person.setId(1);
        var organisationUnit = new OrganisationUnit() {{
            setId(1);
        }};

        when(personService.findPersonByOldId(100)).thenReturn(person);
        when(organisationUnitService.findOrganisationUnitByOldId(100)).thenReturn(organisationUnit);
        when(employmentRepository.save(any())).thenReturn(new Employment());

        // When
        var result = employmentMigrationWorker.performLegacyMigration(request);

        // Then
        assertNotNull(result);
        verify(personService).save(person);
    }

    @Test
    void shouldPerformMigrationWhenPersonFoundByAccountingId() {
        // Given
        var request = new EmploymentMigrationDTO(null, null, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "ACC001", "ACC100");
        var person = new Person();
        person.setId(1);
        var organisationUnit = new OrganisationUnit() {{
            setId(1);
        }};

        when(personService.findPersonByOldId(1)).thenReturn(null);
        when(personService.findPersonByAccountingId("ACC001")).thenReturn(person);
        when(organisationUnitService.findOrganisationUnitByAccountingId("ACC100")).thenReturn(
            organisationUnit);
        when(employmentRepository.save(any())).thenReturn(new Employment());

        // When
        var result = employmentMigrationWorker.performLegacyMigration(request);

        // Then
        assertNotNull(result);
        verify(personService).save(person);
    }

    @Test
    void shouldThrowExceptionWhenPersonNotFound() {
        // Given
        var request = new EmploymentMigrationDTO(1, 1, EmploymentPosition.FULL_PROFESSOR,
            LocalDate.of(2022, 1, 1), "", "");

        when(personService.findPersonByOldId(null)).thenReturn(null);
        when(personService.findPersonByAccountingId("ACC001")).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class,
            () -> employmentMigrationWorker.performLegacyMigration(request));
    }
}
