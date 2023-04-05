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
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.teslaris.core.dto.RegistrationRequestDTO;
import rs.teslaris.core.dto.UserUpdateRequestDTO;
import rs.teslaris.core.exception.CantRegisterAdminException;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.WrongPasswordProvidedException;
import rs.teslaris.core.model.Authority;
import rs.teslaris.core.model.Language;
import rs.teslaris.core.model.OrganisationalUnit;
import rs.teslaris.core.model.Person;
import rs.teslaris.core.model.User;
import rs.teslaris.core.model.UserAccountActivation;
import rs.teslaris.core.repository.AuthorityRepository;
import rs.teslaris.core.repository.UserAccountActivationRepository;
import rs.teslaris.core.repository.UserRepository;
import rs.teslaris.core.service.LanguageService;
import rs.teslaris.core.service.PersonService;
import rs.teslaris.core.service.impl.OrganisationalUnitServiceImpl;
import rs.teslaris.core.service.impl.UserServiceImpl;
import rs.teslaris.core.util.email.EmailUtil;

@SpringBootTest
public class UserServiceTest {

    @Mock
    EmailUtil emailUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LanguageService languageService;
    @Mock
    private AuthorityRepository authorityRepository;
    @Mock
    private UserAccountActivationRepository userAccountActivationRepository;
    @Mock
    private PersonService personService;
    @Mock
    private OrganisationalUnitServiceImpl organisationalUnitService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;


    @Test
    public void shouldDeactivateUserSuccessfully() {
        // given
        var userId = 1;
        var user =
            new User("email@email.com", "passwd", "",
                "Ime", "Prezime", false, true, null,
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
        var userId = 1;
        var user =
            new User("email@email.com", "passwd", "",
                "Ime", "Prezime", false, true, null,
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
        var userId = 0;

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
        registrationRequest.setPersonId(1);
        registrationRequest.setOrganisationalUnitId(1);

        var language = new Language();
        when(languageService.findLanguageById(1)).thenReturn(language);

        Authority authority = new Authority();
        authority.setName("USER");
        when(authorityRepository.findById(2)).thenReturn(Optional.of(authority));

        var person = new Person();
        when(personService.findPersonById(1)).thenReturn(person);

        var organisationalUnit = new OrganisationalUnit();
        when(organisationalUnitService.findOrganisationalUnitById(1)).thenReturn(
            organisationalUnit);

        User newUser = new User("johndoe@example.com", "password123", "",
            "John", "Doe", true,
            false, language, authority, null, null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        // when
        var savedUser = userService.registerUser(registrationRequest);

        // then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredLanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldThrowNotFoundWhenAuthorityNotFound() {
        // given
        var registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setAuthorityId(2);

        when(authorityRepository.findById(2)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.registerUser(registrationRequest));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldThrowCantRegisterAdminWhenTryingToRegisterAdmin() {
        // given
        var registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setAuthorityId(1);
        registrationRequest.setPreferredLanguageId(1);

        var adminAuthority = new Authority();
        adminAuthority.setName("ADMIN");
        when(authorityRepository.findById(1)).thenReturn(Optional.of(adminAuthority));

        var language = new Language();
        when(languageService.findLanguageById(1)).thenReturn(language);

        // when
        assertThrows(CantRegisterAdminException.class,
            () -> userService.registerUser(registrationRequest));

        // then (CantRegisterAdminException should be thrown)
    }

    @Test
    public void shouldActivateUserWhenGivenValidActivationToken() {
        // given
        var activationTokenValue = "valid_token";
        UserAccountActivation accountActivation = new UserAccountActivation(activationTokenValue,
            new User("johndoe@example.com", "password123", "",
                "John", "Doe", true,
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
        var activationTokenValue = "invalid_token";
        when(
            userAccountActivationRepository.findByActivationToken(activationTokenValue)).thenReturn(
            Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> userService.activateUserAccount(activationTokenValue));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldUpdateUserWhenValidInput() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setOldPassword("oldPassword");
        requestDTO.setNewPassword("newPassword");
        requestDTO.setFirstname("John");
        requestDTO.setLastName("Doe");
        requestDTO.setPreferredLanguageId(1);
        requestDTO.setPersonId(2);
        requestDTO.setOrganisationalUnitId(3);

        var user = new User();
        user.setEmail("oldemail@example.com");
        user.setPassword("oldPassword");
        user.setFirstname("Jane");
        user.setLastName("Doe");
        user.setCanTakeRole(false);
        user.setPreferredLanguage(new Language());
        user.setPerson(new Person());
        user.setOrganisationalUnit(new OrganisationalUnit());

        var preferredLanguage = new Language();
        var person = new Person();
        var organisationalUnit = new OrganisationalUnit();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findLanguageById(1)).thenReturn(preferredLanguage);
        when(personService.findPersonById(2)).thenReturn(person);
        when(organisationalUnitService.findOrganisationalUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // when
        userService.updateUser(requestDTO, 1);

        // then
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstname());
        assertEquals("Doe", user.getLastName());
        assertFalse(user.getCanTakeRole());
        assertEquals(preferredLanguage, user.getPreferredLanguage());
        assertEquals(person, user.getPerson());
        assertEquals(organisationalUnit, user.getOrganisationalUnit());
        assertEquals("encodedNewPassword", user.getPassword());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenUserToUpdateNotFound() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.updateUser(requestDTO, 1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldThrowWrongPasswordProvidedExceptionWhenOldPasswordDoesNotMatch() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setOldPassword("wrongPassword");
        var user = new User();
        user.setPassword("currentPassword");
        var preferredLanguage = new Language();
        requestDTO.setPreferredLanguageId(1);
        var person = new Person();
        requestDTO.setPersonId(2);
        var organisationalUnit = new OrganisationalUnit();
        requestDTO.setOrganisationalUnitId(3);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findLanguageById(1)).thenReturn(preferredLanguage);
        when(personService.findPersonById(2)).thenReturn(person);
        when(organisationalUnitService.findOrganisationalUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("wrongPassword", "currentPassword")).thenReturn(false);

        // when
        assertThrows(WrongPasswordProvidedException.class,
            () -> userService.updateUser(requestDTO, 1));

        // then (WrongPasswordProvidedException should be thrown)
    }
}
