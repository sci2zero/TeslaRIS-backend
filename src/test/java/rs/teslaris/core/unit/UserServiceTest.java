package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.RegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.PasswordResetToken;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.PasswordResetTokenRepository;
import rs.teslaris.core.repository.user.RefreshTokenRepository;
import rs.teslaris.core.repository.user.UserAccountActivationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.person.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.user.UserServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.WrongPasswordProvidedException;
import rs.teslaris.core.util.jwt.JwtUtil;

@SpringBootTest
public class UserServiceTest {

    @Mock
    private EmailUtil emailUtil;
    @Mock
    private JwtUtil tokenUtil;
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
    private OrganisationUnitServiceImpl organisationalUnitService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @InjectMocks
    private UserServiceImpl userService;


    @Test
    public void shouldDeactivateUserSuccessfully() {
        // given
        var userId = 1;
        var user =
            new User("email@email.com", "passwd", "",
                "Ime", "Prezime", false, true, null,
                new Authority("RESEARCHER", null), null, null);
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
        registrationRequest.setPersonId(1);
        registrationRequest.setOrganisationalUnitId(1);

        var language = new Language();
        when(languageService.findOne(1)).thenReturn(language);

        Authority authority = new Authority();
        authority.setName(UserRole.RESEARCHER.toString());
        when(authorityRepository.findByName(UserRole.RESEARCHER.toString())).thenReturn(
            Optional.of(authority));

        var person = new Person();
        when(personService.findOne(1)).thenReturn(person);

        var organisationalUnit = new OrganisationUnit();
        when(organisationalUnitService.findOrganisationUnitById(1)).thenReturn(
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

        when(authorityRepository.findById(2)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.registerUser(registrationRequest));

        // then (NotFoundException should be thrown)
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
        user.setOrganisationUnit(new OrganisationUnit());

        var preferredLanguage = new Language();
        var person = new Person();
        var organisationalUnit = new OrganisationUnit();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findOne(1)).thenReturn(preferredLanguage);
        when(personService.findOne(2)).thenReturn(person);
        when(organisationalUnitService.findOrganisationUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // when
        userService.updateUser(requestDTO, 1, "fingerprint");

        // then
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstname());
        assertEquals("Doe", user.getLastName());
        assertFalse(user.getCanTakeRole());
        assertEquals(preferredLanguage, user.getPreferredLanguage());
        assertEquals(person, user.getPerson());
        assertEquals(organisationalUnit, user.getOrganisationUnit());
        assertEquals("encodedNewPassword", user.getPassword());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenUserToUpdateNotFound() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> userService.updateUser(requestDTO, 1, "fingerprint"));

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
        var organisationalUnit = new OrganisationUnit();
        requestDTO.setOrganisationalUnitId(3);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findOne(1)).thenReturn(preferredLanguage);
        when(personService.findOne(2)).thenReturn(person);
        when(organisationalUnitService.findOrganisationUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("wrongPassword", "currentPassword")).thenReturn(false);

        // when
        assertThrows(WrongPasswordProvidedException.class,
            () -> userService.updateUser(requestDTO, 1, "fingerprint"));

        // then (WrongPasswordProvidedException should be thrown)
    }

    @Test
    void shouldReturnTokenForValidUser() {
        // given
        var authenticationManager = mock(AuthenticationManager.class);
        var authenticationRequest = new AuthenticationRequestDTO("test@example.com", "password");
        String fingerprint = "123456";

        var authentication = mock(Authentication.class);
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        when(authenticationManager.authenticate(
            any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        when(tokenUtil.generateToken(any(Authentication.class), eq(fingerprint))).thenReturn(
            "access_token");

        // when
        var response =
            userService.authenticateUser(authenticationManager, authenticationRequest, fingerprint);

        // then
        assertEquals("access_token", response.getToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowBadCredentialsForInvalidUser() {
        // given
        var authenticationManager = mock(AuthenticationManager.class);
        var authenticationRequest =
            new AuthenticationRequestDTO("test@example.com", "password");

        when(authenticationManager.authenticate(
            any(UsernamePasswordAuthenticationToken.class))).thenThrow(
            new BadCredentialsException("Invalid credentials"));

        // when
        assertThrows(BadCredentialsException.class,
            () -> userService.authenticateUser(authenticationManager, authenticationRequest,
                "123456"));

        // then (BadCredentialsException should be thrown)
    }

    @Test
    void shouldReturnTokenWhenTokenIsValid() {
        // given
        var refreshTokenValue = "refresh_token";
        var fingerprint = "123456";
        var refreshToken = new RefreshToken("hashed_refresh_token", new User());

        when(refreshTokenRepository.getRefreshToken(anyString())).thenReturn(
            Optional.of(refreshToken));
        when(tokenUtil.generateToken(any(User.class), eq(fingerprint))).thenReturn("access_token");

        // when
        var response = userService.refreshToken(refreshTokenValue, fingerprint);

        // then
        assertEquals("access_token", response.getToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void shouldThrowNonExistingRefreshTokenExceptionWhenRefreshTokenIsNotValid() {
        // given
        var refreshTokenValue = "refresh_token";
        var refreshTokenRepository = mock(RefreshTokenRepository.class);
        when(refreshTokenRepository.getRefreshToken(anyString())).thenReturn(Optional.empty());

        // when
        assertThrows(NonExistingRefreshTokenException.class,
            () -> userService.refreshToken(refreshTokenValue, "123456"));

        // then (NonExistingRefreshTokenException should be thrown)
    }

    @Test
    public void shouldReturnUserOrganisationUnitIdWhenIdIsValid() {
        // given
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);
        var user = new User();
        user.setOrganisationUnit(organisationUnit);

        when(userRepository.findByIdWithOrganisationUnit(1)).thenReturn(Optional.of(user));

        // when
        int result = userService.getUserOrganisationUnitId(1);

        // then
        assertEquals(organisationUnit.getId(), result);
    }

    @Test
    public void testGetUserOrganisationUnitIdNotFound() {
        // given
        when(userRepository.findByIdWithOrganisationUnit(2)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> {
            userService.getUserOrganisationUnitId(2);
        });

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReturnFalseWhenNoMatch() {
        // given
        var person = new Person();
        person.setId(1);
        var user = new User();
        user.setPerson(person);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // when
        boolean result = userService.isUserAResearcher(1, 2);

        //then
        assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenNoPerson() {
        // given
        var user = new User();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        //when
        boolean result = userService.isUserAResearcher(1, 1);

        //then
        assertFalse(result);
    }

    @Test
    public void shouldSubmitForgottenPassword() {
        // Given
        ForgotPasswordRequestDTO forgotPasswordRequest = new ForgotPasswordRequestDTO();
        forgotPasswordRequest.setUserEmail("test@example.com");

        var user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        userService.initiatePasswordResetProcess(forgotPasswordRequest);

        // Then
        verify(passwordResetTokenRepository, times(1)).save(
            any(PasswordResetToken.class));
        verify(emailUtil, times(1)).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void shouldResetAccountPassword() {
        // Given
        var resetPasswordRequest = new ResetPasswordRequestDTO();
        resetPasswordRequest.setResetToken(UUID.randomUUID().toString());
        resetPasswordRequest.setNewPassword("newPassword");

        User user = new User();
        user.setPassword("oldPassword");

        PasswordResetToken resetRequest =
            new PasswordResetToken(resetPasswordRequest.getResetToken(), user);

        when(passwordResetTokenRepository.findByPasswordResetToken(
            resetPasswordRequest.getResetToken()))
            .thenReturn(Optional.of(resetRequest));
        when(passwordEncoder.encode(resetPasswordRequest.getNewPassword()))
            .thenReturn("encodedPassword");

        // When
        userService.resetAccountPassword(resetPasswordRequest);

        // Then
        assertEquals("encodedPassword", user.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordResetTokenRepository, times(1)).delete(resetRequest);
    }
}
