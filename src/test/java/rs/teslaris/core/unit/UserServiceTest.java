package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.RegistrationRequestDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.RefreshTokenRepository;
import rs.teslaris.core.repository.user.UserAccountActivationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.person.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.user.UserServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;
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
    private UserAccountIndexRepository userAccountIndexRepository;
    @Mock
    private SearchService<UserAccountIndex> searchService;
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
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setPersonId(1);

        var language = new Language();
        when(languageService.findOne(1)).thenReturn(language);

        Authority authority = new Authority();
        authority.setName(UserRole.RESEARCHER.toString());
        when(authorityRepository.findByName(UserRole.RESEARCHER.toString())).thenReturn(
            Optional.of(authority));

        var person = new Person();
        person.setName(new PersonName("John", "Something", "Doe", LocalDate.of(1995, 12, 3), null));
        when(personService.findOne(1)).thenReturn(person);

        when(organisationalUnitService.findOrganisationUnitById(1)).thenReturn(
            new OrganisationUnit());

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag("SR", "Srpski"), "Content", 1)));
        User newUser = new User("johndoe@example.com", "password123", "",
            "John", "Doe", true,
            false, language, authority, null, organisationUnit);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

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
    public void shouldThrowUserAlreadyExistsExceptionWhenUserIsInTheSystem() {
        // Given
        var requestDTO = new RegistrationRequestDTO();
        requestDTO.setEmail("admin@admin.com");
        when(userRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.of(new User()));

        // When
        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(requestDTO));

        // Then (UserAlreadyExistsException should be thrown)
    }

    @Test
    public void shouldUpdateResearcherUserWhenValidInput() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setOldPassword("oldPassword");
        requestDTO.setNewPassword("newPassword");
        requestDTO.setFirstname("JOHN");
        requestDTO.setPreferredLanguageId(1);
        requestDTO.setOrganisationalUnitId(3);

        var user = new User();
        user.setAuthority(new Authority(UserRole.RESEARCHER.toString(), null));
        user.setEmail("oldemail@example.com");
        user.setPassword("oldPassword");
        user.setFirstname("Jane");
        user.setLastName("Doe");
        user.setCanTakeRole(false);
        user.setPreferredLanguage(new Language());
        var person = new Person();
        user.setPerson(person);
        var orgUnit = new OrganisationUnit();
        orgUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag("SR", "Srpski"), "Content", 1)));
        orgUnit.setId(4);
        user.setOrganisationUnit(orgUnit);

        var preferredLanguage = new Language();
        var organisationalUnit = new OrganisationUnit();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findOne(1)).thenReturn(preferredLanguage);
        when(organisationalUnitService.findOrganisationUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // when
        userService.updateUser(requestDTO, 1, "fingerprint");

        // then
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Jane", user.getFirstname());
        assertEquals("Doe", user.getLastName());
        assertFalse(user.getCanTakeRole());
        assertEquals(preferredLanguage, user.getPreferredLanguage());
        assertEquals(person, user.getPerson());
        assertNotEquals(organisationalUnit, user.getOrganisationUnit());
        assertEquals("encodedNewPassword", user.getPassword());
    }

    @Test
    public void shouldUpdateEmployeeUserWhenValidInput() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setOldPassword("oldPassword");
        requestDTO.setNewPassword("newPassword");
        requestDTO.setFirstname("JOHN");
        requestDTO.setLastName("SMITH");
        requestDTO.setPreferredLanguageId(1);
        requestDTO.setOrganisationalUnitId(3);

        var user = new User();
        user.setAuthority(new Authority(UserRole.INSTITUTIONAL_EDITOR.toString(), null));
        user.setEmail("oldemail@example.com");
        user.setPassword("oldPassword");
        user.setFirstname("Jane");
        user.setLastName("Doe");
        user.setCanTakeRole(false);
        user.setPreferredLanguage(new Language());
        var person = new Person();
        user.setPerson(person);
        var orgUnit = new OrganisationUnit();
        orgUnit.setId(4);
        user.setOrganisationUnit(orgUnit);

        var preferredLanguage = new Language();
        var organisationalUnit = new OrganisationUnit();
        organisationalUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag("SR", "Srpski"), "Content", 1)));

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findOne(1)).thenReturn(preferredLanguage);
        when(organisationalUnitService.findOne(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // when
        userService.updateUser(requestDTO, 1, "fingerprint");

        // then
        assertEquals("test@example.com", user.getEmail());
        assertEquals("JOHN", user.getFirstname());
        assertEquals("SMITH", user.getLastName());
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
        user.setAuthority(new Authority(UserRole.RESEARCHER.toString(), null));
        user.setPassword("currentPassword");
        var preferredLanguage = new Language();
        requestDTO.setPreferredLanguageId(1);
        var organisationalUnit = new OrganisationUnit();
        requestDTO.setOrganisationalUnitId(3);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageService.findOne(1)).thenReturn(preferredLanguage);
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
    public void shouldGetUserOrganisationUnitIdNotFound() {
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
    public void shouldFindUserAccountWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new UserAccountIndex(), new UserAccountIndex())));

        // when
        var result =
            userService.searchUserAccounts(new ArrayList<>(tokens), pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldUpdateResearcherCurrentOrganisationUnitIfBound() {
        // Given
        var personId = 123;
        var person = new Person();
        var orgUnit = new OrganisationUnit();
        orgUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag("SR", "Srpski"), "Content", 1)));
        var involvement = new Involvement();
        involvement.setInvolvementType(InvolvementType.EMPLOYED_AT);
        involvement.setOrganisationUnit(orgUnit);
        person.setInvolvements(Set.of(involvement));
        var userToUpdate = new User();
        userToUpdate.setAuthority(new Authority());
        userToUpdate.setPerson(person);

        when(personService.findOne(personId)).thenReturn(person);
        when(userRepository.findForResearcher(personId)).thenReturn(Optional.of(userToUpdate));

        // When
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);

        // Then
        verify(personService, times(1)).findOne(personId);
        verify(userRepository, times(1)).findForResearcher(personId);
        verify(userRepository, times(1)).save(userToUpdate);
        verify(userAccountIndexRepository, times(1)).save(any());
    }
}
