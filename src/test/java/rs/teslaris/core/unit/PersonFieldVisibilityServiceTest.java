package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.person.PersonFieldVisibilityDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonFieldVisibility;
import rs.teslaris.core.repository.person.PersonFieldVisibilityRepository;
import rs.teslaris.core.service.impl.person.PersonFieldVisibilityServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class PersonFieldVisibilityServiceTest {

    @Mock
    private PersonFieldVisibilityRepository personFieldVisibilityRepository;

    @Mock
    private PersonService personService;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @InjectMocks
    private PersonFieldVisibilityServiceImpl service;


    @Test
    public void shouldReadPublicFieldConfigurationWhenConfigurationExists() {
        // Given
        int personId = 123;
        var existingConfig = new PersonFieldVisibility();
        existingConfig.setPhoneNumberVisible(true);
        existingConfig.setContactEmailVisible(false);
        existingConfig.setDateOfBirthVisible(true);
        existingConfig.setSexVisible(false);
        existingConfig.setBirthplaceVisible(true);

        when(personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId))
            .thenReturn(Optional.of(existingConfig));

        // When
        var result = service.readPublicFieldConfiguration(personId);

        // Then
        assertNotNull(result);
        assertTrue(result.phoneNumberVisible());
        assertFalse(result.contactEmailVisible());
        assertTrue(result.dateOfBirthVisible());
        assertFalse(result.sexVisible());
        assertTrue(result.birthplaceVisible());
        verify(personFieldVisibilityRepository).getFieldVisibilityConfiguration(personId);
    }

    @Test
    public void shouldReadPublicFieldConfigurationWhenConfigurationDoesNotExist() {
        // Given
        int personId = 456;

        when(personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId))
            .thenReturn(Optional.empty());

        // When
        var result = service.readPublicFieldConfiguration(personId);

        // Then
        assertNotNull(result);
        assertFalse(result.phoneNumberVisible());
        assertFalse(result.contactEmailVisible());
        assertFalse(result.dateOfBirthVisible());
        assertFalse(result.sexVisible());
        assertFalse(result.birthplaceVisible());
        verify(personFieldVisibilityRepository).getFieldVisibilityConfiguration(personId);
    }

    @Test
    public void shouldSavePublicFieldConfigurationWhenConfigurationExists() {
        // Given
        int personId = 789;
        var person = new Person();
        person.setId(personId);

        var dto = new PersonFieldVisibilityDTO(
            true,  // phoneNumberVisible
            false, // contactEmailVisible
            true,  // dateOfBirthVisible
            false, // sexVisible
            true   // birthplaceVisible
        );

        var existingConfig = new PersonFieldVisibility();
        existingConfig.setPerson(person);
        existingConfig.setPhoneNumberVisible(false);
        existingConfig.setContactEmailVisible(true);
        existingConfig.setDateOfBirthVisible(false);
        existingConfig.setSexVisible(true);
        existingConfig.setBirthplaceVisible(false);

        when(personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId))
            .thenReturn(Optional.of(existingConfig));
        when(personFieldVisibilityRepository.save(any(PersonFieldVisibility.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(personIndexRepository.findByDatabaseId(any())).thenReturn(
            Optional.of(new PersonIndex()));

        // When
        service.savePublicFieldConfiguration(personId, dto);

        // Then
        verify(personFieldVisibilityRepository).save(argThat(config ->
            config.getPerson().equals(person) &&
                config.getPhoneNumberVisible() == dto.phoneNumberVisible() &&
                config.getContactEmailVisible() == dto.contactEmailVisible() &&
                config.getDateOfBirthVisible() == dto.dateOfBirthVisible() &&
                config.getSexVisible() == dto.sexVisible() &&
                config.getBirthplaceVisible() == dto.birthplaceVisible()
        ));
        verify(personService, never()).findOne(anyInt());
        verify(personIndexRepository, times(1)).save(any());
    }

    @Test
    public void shouldSavePublicFieldConfigurationWhenConfigurationDoesNotExist() {
        // Given
        int personId = 999;
        var person = new Person();
        person.setId(personId);

        var dto = new PersonFieldVisibilityDTO(
            false, // phoneNumberVisible
            true,  // contactEmailVisible
            false, // dateOfBirthVisible
            true,  // sexVisible
            false  // birthplaceVisible
        );

        when(personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId))
            .thenReturn(Optional.empty());
        when(personService.findOne(personId))
            .thenReturn(person);
        when(personFieldVisibilityRepository.save(any(PersonFieldVisibility.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(personIndexRepository.findByDatabaseId(any())).thenReturn(
            Optional.of(new PersonIndex()));

        // When
        service.savePublicFieldConfiguration(personId, dto);

        // Then
        verify(personFieldVisibilityRepository).save(argThat(config ->
            config.getPerson().equals(person) &&
                config.getPhoneNumberVisible() == dto.phoneNumberVisible() &&
                config.getContactEmailVisible() == dto.contactEmailVisible() &&
                config.getDateOfBirthVisible() == dto.dateOfBirthVisible() &&
                config.getSexVisible() == dto.sexVisible() &&
                config.getBirthplaceVisible() == dto.birthplaceVisible()
        ));
        verify(personService).findOne(personId);
        verify(personIndexRepository, times(1)).save(any());
    }
}
