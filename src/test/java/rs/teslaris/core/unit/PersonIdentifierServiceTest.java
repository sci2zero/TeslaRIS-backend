package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.identifier.PersonIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.PersonIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.PersonIdentifierServiceImpl;
import rs.teslaris.core.service.impl.identifier.cruddelegate.PersonIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class PersonIdentifierServiceTest {

    @Mock
    private PersonIdentifierRepository personIdentifierRepository;

    @Mock
    private PersonIdentifierJPAServiceImpl personIdentifierJPAService;

    @Mock
    private PersonService personService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private PersonIdentifierServiceImpl personIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllPersonIdentifiersForPerson(AccessLevel accessLevel) {
        // Given
        var personId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.EVENT)));

        var personIdentifier1 = new PersonIdentifier();
        personIdentifier1.setIdentifier(identifier);

        var personIdentifier2 = new PersonIdentifier();
        personIdentifier2.setIdentifier(identifier);

        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(personId,
            accessLevel)).thenReturn(List.of(personIdentifier1, personIdentifier2));

        // When
        var response = personIdentifierService.getIdentifiersForPerson(personId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(personIdentifierRepository)
            .findIdentifiersForPersonAndIdentifierAccessLevel(personId, accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForPerson() {
        // Given
        var personId = 1;

        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(personId,
            AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response = personIdentifierService.getIdentifiersForPerson(personId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreatePersonIdentifier() {
        // Given
        var personIdentifierDTO = new PersonIdentifierDTO();
        personIdentifierDTO.setPersonId(1);
        personIdentifierDTO.setIdentifierId(1);
        personIdentifierDTO.setValue("10.1234/test");

        var newPersonIdentifier = new PersonIdentifier();
        newPersonIdentifier.setIdentifier(new Identifier());

        when(personService.findOne(1)).thenReturn(new Person());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(personIdentifierJPAService.save(any(PersonIdentifier.class)))
            .thenReturn(newPersonIdentifier);

        // When
        var result = personIdentifierService.createPersonIdentifier(personIdentifierDTO, 1);

        // Then
        assertNotNull(result);
        verify(personService).findOne(1);
        verify(personIdentifierJPAService).save(any(PersonIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingPersonIdentifierWithNonExistentPerson() {
        // Given
        var personIdentifierDTO = new PersonIdentifierDTO();
        personIdentifierDTO.setPersonId(99);
        personIdentifierDTO.setIdentifierId(1);

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(personService.findOne(99)).thenThrow(new NotFoundException("Person not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            personIdentifierService.createPersonIdentifier(personIdentifierDTO, 1));

        verify(personIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdatePersonIdentifier() {
        // Given
        var personIdentifierId = 1;
        var personIdentifierDTO = new PersonIdentifierDTO();
        personIdentifierDTO.setPersonId(1);
        personIdentifierDTO.setIdentifierId(1);
        personIdentifierDTO.setValue("10.1234/updated");

        var existingPersonIdentifier = new PersonIdentifier();
        existingPersonIdentifier.setIdentifier(new Identifier());

        when(personIdentifierJPAService.findOne(personIdentifierId))
            .thenReturn(existingPersonIdentifier);
        when(personService.findOne(1)).thenReturn(new Person());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        personIdentifierService.updatePersonIdentifier(personIdentifierId, personIdentifierDTO);

        // Then
        verify(personIdentifierJPAService).findOne(personIdentifierId);
        verify(personService).findOne(1);
        verify(personIdentifierJPAService).save(existingPersonIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentPersonIdentifier() {
        // Given
        var personIdentifierId = 99;
        var personIdentifierDTO = new PersonIdentifierDTO();
        personIdentifierDTO.setPersonId(1);
        personIdentifierDTO.setIdentifierId(1);

        when(personIdentifierJPAService.findOne(personIdentifierId))
            .thenThrow(new NotFoundException("Person identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            personIdentifierService.updatePersonIdentifier(personIdentifierId,
                personIdentifierDTO));

        verify(personIdentifierJPAService, never()).save(any());
    }
}
