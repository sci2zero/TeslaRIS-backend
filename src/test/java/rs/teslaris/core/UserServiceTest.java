package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.RegistrationRequestDTO;
import rs.teslaris.core.exception.CantRegisterAdminException;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.Authority;
import rs.teslaris.core.model.Language;
import rs.teslaris.core.model.User;
import rs.teslaris.core.model.UserAccountActivation;
import rs.teslaris.core.repository.AuthorityRepository;
import rs.teslaris.core.repository.LanguageRepository;
import rs.teslaris.core.repository.UserAccountActivationRepository;
import rs.teslaris.core.repository.UserRepository;
import rs.teslaris.core.service.impl.UserServiceImpl;

@SpringBootTest
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private UserAccountActivationRepository userAccountActivationRepository;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    public void shouldDeactivateUserSuccessfully() {
        // given
        Integer userId = 1;
        User user =
            new User("email@email.com", "passwd", "", "Ime", "Prezime", false, true, null,
                new Authority("AUTHOR", null), null, null);
        user.setId(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deactivateUser(userId);

        // then
        assertTrue(user.getLocked());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void shouldNotDeactivateAdminUser() {
        // given
        Integer userId = 1;
        User user =
            new User("email@email.com", "passwd", "", "Ime", "Prezime", false, true, null,
                new Authority("ADMIN", null), null, null);
        user.setId(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deactivateUser(userId);

        // then
        assertFalse(user.getLocked());
        verify(userRepository, never()).save(user);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenUserNotFound() {
        // given
        Integer userId = 0;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.deactivateUser(userId));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldRegisterUserWithValidData() {
        // given
        var registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setEmail("johndoe@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setFirstname("John");
        registrationRequest.setLastName("Doe");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setAuthorityId(2);

        var language = new Language();
        language.setId(1);
        when(languageRepository.findById(1)).thenReturn(Optional.of(language));

        Authority authority = new Authority();
        authority.setId(2);
        authority.setName("USER");
        when(authorityRepository.findById(2)).thenReturn(Optional.of(authority));

        User newUser = new User("johndoe@example.com", "password123", "", "John", "Doe", true,
            false, language, authority, null, null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        // when
        User savedUser = userService.registerUser(registrationRequest);

        // then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredLanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldThrowNotFoundWhenLanguageNotFound() {
        // given
        RegistrationRequestDTO registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setPreferredLanguageId(1);

        when(languageRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.registerUser(registrationRequest));
    }

    @Test
    public void shouldThrowNotFoundWhenAuthorityNotFound() {
        // given
        RegistrationRequestDTO registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setAuthorityId(2);

        when(authorityRepository.findById(2)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.registerUser(registrationRequest));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldThrowCantRegisterAdminWhenTryingToRegisterAdmin() {
        // given
        RegistrationRequestDTO registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setAuthorityId(1);
        registrationRequest.setPreferredLanguageId(1);

        Authority adminAuthority = new Authority();
        adminAuthority.setId(1);
        adminAuthority.setName("ADMIN");
        when(authorityRepository.findById(1)).thenReturn(Optional.of(adminAuthority));

        var language = new Language();
        language.setId(1);
        when(languageRepository.findById(1)).thenReturn(Optional.of(language));

        // when
        assertThrows(CantRegisterAdminException.class,
            () -> userService.registerUser(registrationRequest));

        // then (CantRegisterAdminException should be thrown)
    }

    @Test
    public void shouldActivateUserWhenGivenValidActivationToken() {
        // given
        String activationTokenValue = "valid_token";
        UserAccountActivation accountActivation = new UserAccountActivation(activationTokenValue,
            new User("johndoe@example.com", "password123", "", "John", "Doe", true,
                true, new Language(), new Authority(), null, null));
        when(
            userAccountActivationRepository.findByActivationToken(activationTokenValue)).thenReturn(
            Optional.of(accountActivation));

        // when
        userService.activateUserAccount(activationTokenValue);

        // then
        verify(userRepository, times(1)).save(accountActivation.getUser());
        verify(userAccountActivationRepository, times(1)).delete(accountActivation);
        assertFalse(accountActivation.getUser().getLocked());
    }

    @Test
    public void shouldThrowNotFoundWhenProvidingInvalidActivationToken() {
        // given
        String activationTokenValue = "invalid_token";
        when(
            userAccountActivationRepository.findByActivationToken(activationTokenValue)).thenReturn(
            Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> userService.activateUserAccount(activationTokenValue));

        // then (NotFoundException should be thrown)
    }
}
