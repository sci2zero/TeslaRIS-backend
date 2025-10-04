package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.configuration.OAuth2Provider;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.CommissionRegistrationRequestDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.EmailUpdateRequest;
import rs.teslaris.core.model.user.OAuthCode;
import rs.teslaris.core.model.user.PasswordResetToken;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.EmailUpdateRequestRepository;
import rs.teslaris.core.repository.user.OAuthCodeRepository;
import rs.teslaris.core.repository.user.PasswordResetTokenRepository;
import rs.teslaris.core.repository.user.RefreshTokenRepository;
import rs.teslaris.core.repository.user.UserAccountActivationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.institution.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.user.UserServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidOAuth2CodeException;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PasswordException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;

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
    private LanguageTagService languageTagService;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private UserAccountActivationRepository userAccountActivationRepository;

    @Mock
    private PersonService personService;

    @Mock
    private OrganisationUnitServiceImpl organisationUnitService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private Cache<String, Byte> passwordResetRequestCacheStore;

    @Mock
    private MessageSource messageSource;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private EmailUpdateRequestRepository emailUpdateRequestRepository;

    @Mock
    private OAuthCodeRepository oAuthCodeRepository;

    @Mock
    private BrandingInformationService brandingInformationService;

    @InjectMocks
    private UserServiceImpl userService;

    private LanguageTag language;

    private Authority authority;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(userService, "clientAppAddress", "protocol://test.test/");

        language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.SERBIAN);

        authority = new Authority();
        authority.setName(UserRole.RESEARCHER.toString());

        // Default mocks
        when(languageTagService.findOne(anyInt())).thenReturn(language);
        when(authorityRepository.findByName(UserRole.RESEARCHER.toString()))
            .thenReturn(Optional.of(authority));
        when(passwordEncoder.encode(anyString())).thenReturn("EncodedPassword");
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Message");
    }

    @Test
    public void shouldDeactivateUserSuccessfully() {
        // given
        var userId = 1;
        var user =
            new User("email@email.com", "passwd", "",
                "Ime", "Prezime", false, true, null, null,
                new Authority("RESEARCHER", null), null, null, null, UserNotificationPeriod.NEVER);
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
                "Ime", "Prezime", false, true, null, null,
                new Authority("ADMIN", null), null, null, null, UserNotificationPeriod.NEVER);
        user.setId(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThrows(CantEditException.class, () -> userService.deactivateUser(userId));
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
    public void shouldRegisterResearcherWithValidData() {
        // given
        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("johndoe@example.com");
        registrationRequest.setPassword("Password123");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setPersonId(1);
        registrationRequest.setOrganisationUnitId(1);

        var language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.SERBIAN);
        when(languageTagService.findOne(1)).thenReturn(language);

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));

        var authority = new Authority();
        authority.setName(UserRole.RESEARCHER.toString());
        when(authorityRepository.findByName(UserRole.RESEARCHER.toString())).thenReturn(
            Optional.of(authority));

        var person = new Person();
        person.setName(new PersonName("John", "Something", "Doe", LocalDate.of(1995, 12, 3), null));
        when(personService.findOne(1)).thenReturn(person);
        when(organisationUnitService.findOrganisationUnitById(1)).thenReturn(
            new OrganisationUnit());

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        var newUser = new User("johndoe@example.com", "Password123", "",
            "John", "Doe", true,
            false, language, language, authority, null, organisationUnit, null,
            UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));
        when(organisationUnitService.findOne(anyInt())).thenReturn(new OrganisationUnit() {{
            setIsClientInstitution(true);
            setValidateEmailDomain(false);
        }});

        // when
        var savedUser = userService.registerResearcher(registrationRequest);

        // then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredReferenceCataloguingLanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldRegisterEmployeeWithValidData() throws NoSuchAlgorithmException {
        // Given
        var registrationRequest = new EmployeeRegistrationRequestDTO();
        registrationRequest.setEmail("johndoe@example.com");
        registrationRequest.setNote("note note note");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setOrganisationUnitId(1);
        registrationRequest.setName("Name");
        registrationRequest.setSurname("Surname");

        var language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.SERBIAN);
        when(languageTagService.findOne(1)).thenReturn(language);

        var authority = new Authority();
        authority.setName(UserRole.INSTITUTIONAL_EDITOR.toString());
        when(authorityRepository.findByName(UserRole.INSTITUTIONAL_EDITOR.toString())).thenReturn(
            Optional.of(authority));

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);

        var newUser = new User("johndoe@example.com", "password123", "",
            "John", "Doe", true,
            false, language, language, authority, null, organisationUnit, null,
            UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // When
        var savedUser = userService.registerInstitutionEmployee(registrationRequest,
            UserRole.INSTITUTIONAL_EDITOR);

        // Then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredUILanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldRegisterViceDeanForScienceWithValidData() throws NoSuchAlgorithmException {
        // Given
        var registrationRequest = new EmployeeRegistrationRequestDTO();
        registrationRequest.setEmail("johndoe@example.com");
        registrationRequest.setNote("note note note");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setOrganisationUnitId(1);
        registrationRequest.setName("Name");
        registrationRequest.setSurname("Surname");

        var language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.SERBIAN);
        when(languageTagService.findOne(1)).thenReturn(language);

        var authority = new Authority();
        authority.setName(UserRole.VICE_DEAN_FOR_SCIENCE.toString());
        when(authorityRepository.findByName(UserRole.VICE_DEAN_FOR_SCIENCE.toString())).thenReturn(
            Optional.of(authority));

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);

        var newUser = new User("johndoe@example.com", "password123", "",
            "John", "Doe", true,
            false, language, language, authority, null, organisationUnit, null,
            UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // When
        var savedUser = userService.registerInstitutionEmployee(registrationRequest,
            UserRole.VICE_DEAN_FOR_SCIENCE);

        // Then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredUILanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldRegisterPromotionRegistryAdminWithValidData()
        throws NoSuchAlgorithmException {
        // Given
        var registrationRequest = new EmployeeRegistrationRequestDTO();
        registrationRequest.setEmail("regadmin@example.com");
        registrationRequest.setNote("note note note");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setOrganisationUnitId(1);
        registrationRequest.setName("Promotion");
        registrationRequest.setSurname("Admin");

        var language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.ENGLISH);
        when(languageTagService.findOne(1)).thenReturn(language);

        var authority = new Authority();
        authority.setName(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString());
        when(authorityRepository.findByName(
            UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString())).thenReturn(
            Optional.of(authority));

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(
                new MultiLingualContent(new LanguageTag(LanguageAbbreviations.ENGLISH, "English"),
                    "University", 1)));
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);

        var newUser = new User("regadmin@example.com", "password123", "",
            "Promotion", "Admin", true,
            false, language, language, authority, null, organisationUnit, null,
            UserNotificationPeriod.WEEKLY);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));

        // When
        var savedUser = userService.registerInstitutionEmployee(registrationRequest,
            UserRole.PROMOTION_REGISTRY_ADMINISTRATOR);

        // Then
        assertNotNull(savedUser);
        assertEquals("regadmin@example.com", savedUser.getEmail());
        assertEquals("Promotion", savedUser.getFirstname());
        assertEquals("Admin", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredReferenceCataloguingLanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldRegisterCommissionUserWithValidData() throws NoSuchAlgorithmException {
        // Given
        var registrationRequest = new CommissionRegistrationRequestDTO();
        registrationRequest.setEmail("johndoe@example.com");
        registrationRequest.setNote("note note note");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setOrganisationUnitId(1);
        registrationRequest.setCommissionId(1);
        registrationRequest.setName("Name");
        registrationRequest.setSurname("Surname");

        var language = new LanguageTag();
        language.setLanguageTag(LanguageAbbreviations.SERBIAN);
        when(languageTagService.findOne(1)).thenReturn(language);

        var authority = new Authority();
        authority.setName(UserRole.COMMISSION.toString());
        when(authorityRepository.findByName(UserRole.COMMISSION.toString())).thenReturn(
            Optional.of(authority));

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);

        when(commissionRepository.findById(1)).thenReturn(Optional.of(new Commission()));

        User newUser = new User("johndoe@example.com", "password123", "",
            "John", "Doe", true,
            false, language, language, authority, null, organisationUnit, null,
            UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        when(userAccountActivationRepository.save(any(UserAccountActivation.class))).thenReturn(
            activationToken);

        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));

        // When
        var savedUser = userService.registerCommissionUser(registrationRequest);

        // Then
        assertNotNull(savedUser);
        assertEquals("johndoe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(language, savedUser.getPreferredUILanguage());
        assertEquals(authority, savedUser.getAuthority());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenAuthorityNotFound() {
        // given
        ReflectionTestUtils.setField(userService, "allowNewResearcherCreation", true);

        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setPassword("Password123");

        when(authorityRepository.findByName(anyString())).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> userService.registerResearcher(registrationRequest));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldThrowPasswordExceptionWhenPasswordIsWeak() {
        // given
        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setPassword("weak_password");

        when(authorityRepository.findById(2)).thenReturn(Optional.empty());

        // when
        assertThrows(PasswordException.class,
            () -> userService.registerResearcher(registrationRequest));

        // then (PasswordException should be thrown)
    }

    @Test
    public void shouldActivateUserWhenGivenValidActivationToken() {
        // given
        var activationTokenValue = "valid_token";
        UserAccountActivation accountActivation = new UserAccountActivation(activationTokenValue,
            new User("johndoe@example.com", "password123", "",
                "John", "Doe", true,
                true, new LanguageTag(), new LanguageTag(), new Authority(), null, null, null,
                UserNotificationPeriod.NEVER));
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
        var requestDTO = new ResearcherRegistrationRequestDTO();
        requestDTO.setEmail("admin@admin.com");
        when(userRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.of(new User()));

        // When
        assertThrows(UserAlreadyExistsException.class,
            () -> userService.registerResearcher(requestDTO));

        // Then (UserAlreadyExistsException should be thrown)
    }

    @Test
    public void shouldUpdateResearcherUserWhenValidInput() {
        // given
        var requestDTO = new UserUpdateRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setOldPassword("oldPassword");
        requestDTO.setNewPassword("newPassword123");
        requestDTO.setFirstname("JOHN");
        requestDTO.setPreferredUILanguageTagId(1);
        requestDTO.setOrganisationalUnitId(3);
        requestDTO.setNotificationPeriod(UserNotificationPeriod.WEEKLY);

        var user = new User();
        user.setAuthority(new Authority(UserRole.RESEARCHER.toString(), null));
        user.setEmail("oldemail@example.com");
        user.setPassword("oldPassword");
        user.setFirstname("Jane");
        user.setLastName("Doe");
        user.setLocked(false);
        user.setCanTakeRole(false);
        user.setPreferredUILanguage(new LanguageTag());
        user.setPreferredReferenceCataloguingLanguage(new LanguageTag());
        var person = new Person();
        user.setPerson(person);
        var orgUnit = new OrganisationUnit();
        orgUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        orgUnit.setId(4);
        user.setOrganisationUnit(orgUnit);

        var preferredLanguage = new LanguageTag();
        preferredLanguage.setLanguageTag("SR");
        var organisationalUnit = new OrganisationUnit();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageTagService.findOne(1)).thenReturn(preferredLanguage);
        when(organisationUnitService.findOrganisationUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // when
        userService.updateUser(requestDTO, 1, "fingerprint");

        // then
        assertEquals("oldemail@example.com", user.getEmail());
        assertEquals("Jane", user.getFirstname());
        assertEquals("Doe", user.getLastName());
        assertFalse(user.getCanTakeRole());
        assertEquals(preferredLanguage, user.getPreferredUILanguage());
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
        requestDTO.setNewPassword("newPassword123");
        requestDTO.setFirstname("JOHN");
        requestDTO.setLastName("SMITH");
        requestDTO.setPreferredUILanguageTagId(1);
        requestDTO.setPreferredReferenceCataloguingLanguageTagId(1);
        requestDTO.setOrganisationalUnitId(3);
        requestDTO.setNotificationPeriod(UserNotificationPeriod.DAILY);

        var user = new User();
        user.setAuthority(new Authority(UserRole.INSTITUTIONAL_EDITOR.toString(), null));
        user.setEmail("oldemail@example.com");
        user.setPassword("oldPassword");
        user.setFirstname("Jane");
        user.setLastName("Doe");
        user.setCanTakeRole(false);
        user.setLocked(false);
        user.setPreferredUILanguage(new LanguageTag());
        user.setPreferredReferenceCataloguingLanguage(new LanguageTag());
        var person = new Person();
        user.setPerson(person);
        var orgUnit = new OrganisationUnit();
        orgUnit.setId(4);
        user.setOrganisationUnit(orgUnit);

        var preferredLanguage = new LanguageTag();
        preferredLanguage.setLanguageTag("SR");
        var organisationalUnit = new OrganisationUnit();
        organisationalUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageTagService.findOne(1)).thenReturn(preferredLanguage);
        when(organisationUnitService.findOne(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("oldPassword", "oldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userAccountIndexRepository.findByDatabaseId(1)).thenReturn(
            Optional.of(new UserAccountIndex()));

        // when
        userService.updateUser(requestDTO, 1, "fingerprint");

        // then
        assertEquals("oldemail@example.com", user.getEmail());
        assertEquals("JOHN", user.getFirstname());
        assertEquals("SMITH", user.getLastName());
        assertFalse(user.getCanTakeRole());
        assertEquals(preferredLanguage, user.getPreferredReferenceCataloguingLanguage());
        assertEquals(person, user.getPerson());
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
        requestDTO.setNotificationPeriod(UserNotificationPeriod.WEEKLY);
        requestDTO.setEmail("new.email@example.com");
        var user = new User();
        user.setAuthority(new Authority(UserRole.RESEARCHER.toString(), null));
        user.setPassword("currentPassword");
        user.setEmail("old.email@example.com");
        var preferredLanguage = new LanguageTag();
        preferredLanguage.setLanguageTag("SR");
        requestDTO.setPreferredUILanguageTagId(1);
        var organisationalUnit = new OrganisationUnit();
        requestDTO.setOrganisationalUnitId(3);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(languageTagService.findOne(1)).thenReturn(preferredLanguage);
        when(organisationUnitService.findOrganisationUnitById(3)).thenReturn(
            organisationalUnit);
        when(passwordEncoder.matches("wrongPassword", "currentPassword")).thenReturn(false);

        // when
        assertThrows(PasswordException.class,
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
    public void shouldSubmitForgottenPassword() {
        // Given
        ForgotPasswordRequestDTO forgotPasswordRequest = new ForgotPasswordRequestDTO();
        forgotPasswordRequest.setUserEmail("test@example.com");

        var user = new User();
        user.setEmail("test@example.com");
        user.setPreferredUILanguage(
            new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        userService.initiatePasswordResetProcess(forgotPasswordRequest);

        // Then
        verify(passwordResetTokenRepository, times(1)).save(
            any(PasswordResetToken.class));
        verify(emailUtil, times(1)).sendSimpleEmail(any(), any(), any());
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

    @ParameterizedTest
    @EnumSource(value = UserRole.class)
    public void shouldFindUserAccountWhenSearchingWithSimpleQuery(UserRole allowedRole) {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new UserAccountIndex(), new UserAccountIndex())));

        // when
        var result =
            userService.searchUserAccounts(new ArrayList<>(tokens), List.of(allowedRole), pageable);

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
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        var involvement = new Involvement();
        involvement.setInvolvementType(InvolvementType.EMPLOYED_AT);
        involvement.setOrganisationUnit(orgUnit);
        person.setInvolvements(Set.of(involvement));
        var userToUpdate = new User();
        userToUpdate.setAuthority(new Authority());
        userToUpdate.setPerson(person);
        userToUpdate.setLocked(false);

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

    @Test
    void shouldGetUserFromPersonWhenExistingPerson() {
        // Given
        var personId = 1;
        var user = new User();
        user.setPreferredUILanguage(new LanguageTag());
        user.setPreferredReferenceCataloguingLanguage(new LanguageTag());

        when(userRepository.findForResearcher(personId)).thenReturn(Optional.of(user));

        // When
        var result = userService.getUserFromPerson(personId);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenNonExistingPerson() {
        // Given
        var personId = 1;

        when(userRepository.findForResearcher(personId)).thenReturn(Optional.empty());

        // When
        assertThrows(NotFoundException.class, () -> userService.getUserFromPerson(personId));

        // Then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReindexUsers() {
        // Given
        var user1 = new User();
        user1.setAuthority(new Authority("ADMIN", new HashSet<>()));
        user1.setLocked(false);
        var user2 = new User();
        user2.setAuthority(new Authority("RESEARCHER", new HashSet<>()));
        user2.setLocked(false);
        var user3 = new User();
        user3.setAuthority(new Authority("INSTITUTIONAL_EDITOR", new HashSet<>()));
        user3.setLocked(false);
        var users = Arrays.asList(user1, user2, user3);
        var page1 = new PageImpl<>(users.subList(0, 2), PageRequest.of(0, 10), users.size());
        var page2 = new PageImpl<>(users.subList(2, 3), PageRequest.of(1, 10), users.size());

        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        userService.reindexUsers();

        // Then
        verify(userAccountIndexRepository, times(1)).deleteAll();
        verify(userRepository, atLeastOnce()).findAll(any(PageRequest.class));
        verify(userAccountIndexRepository, atLeastOnce()).save(any(UserAccountIndex.class));
    }

    @Test
    public void shouldGetAccountsWithRoleTakingAllowed() {
        // Given
        when(userRepository.getIdsOfUsersWhoAllowedAccountTakeover()).thenReturn(List.of(1, 2, 3));

        // When
        var result = userService.getAccountsWithRoleTakingAllowed();

        // Then
        verify(userRepository, times(1)).getIdsOfUsersWhoAllowedAccountTakeover();
        assertEquals(3, result.size());
    }

    @Test
    void shouldThrowExceptionWhenDeletingAdminUser() {
        // Given
        var user = new User();
        user.setId(1);
        user.setAuthority(new Authority() {{
            setName(UserRole.ADMIN.name());
        }});
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // When / Then
        assertThrows(RuntimeException.class, () -> userService.deleteUserAccount(1));
    }

    @Test
    void shouldThrowExceptionWhenDeletingCommissionUserWithIndicators() {
        // Given
        var user = new User();
        user.setId(2);
        user.setAuthority(new Authority() {{
            setName(UserRole.COMMISSION.name());
        }});
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(userRepository.hasUserAssignedIndicators(2)).thenReturn(true);

        // When / Then
        assertThrows(RuntimeException.class, () -> userService.deleteUserAccount(2));
    }

    @Test
    void shouldDeleteUser() {
        // Given
        var user = new User();
        user.setId(3);
        user.setAuthority(new Authority() {{
            setName(UserRole.INSTITUTIONAL_LIBRARIAN.name());
        }});
        when(userRepository.findById(3)).thenReturn(Optional.of(user));

        // When
        userService.deleteUserAccount(3);

        // Then
        verify(userRepository).deleteAllNotificationsForUser(3);
        verify(userRepository).deleteAllAccountActivationsForUser(3);
        verify(userRepository).deleteAllPasswordResetsForUser(3);
        verify(userRepository).deleteRefreshTokenForUser(3);
        verify(userRepository).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenMigratingNonCommissionUsers() {
        // Given
        var user1 = new User();
        user1.setId(4);
        user1.setAuthority(new Authority() {{
            setName(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.name());
        }});

        var user2 = new User();
        user2.setId(5);
        user2.setAuthority(new Authority() {{
            setName(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.name());
        }});

        when(userRepository.findById(4)).thenReturn(Optional.of(user1));
        when(userRepository.findById(5)).thenReturn(Optional.of(user2));

        // When / Then
        assertThrows(RuntimeException.class,
            () -> userService.migrateUserAccountData(4, 5));
    }

    @Test
    void shouldMigrateCommissionUserAccountData() {
        // Given
        var user1 = new User();
        user1.setId(6);
        user1.setAuthority(new Authority() {{
            setName(UserRole.COMMISSION.name());
        }});

        var user2 = new User();
        user2.setId(7);
        user2.setAuthority(new Authority() {{
            setName(UserRole.COMMISSION.name());
        }});

        when(userRepository.findById(6)).thenReturn(Optional.of(user1));
        when(userRepository.findById(7)).thenReturn(Optional.of(user2));

        // When
        userService.migrateUserAccountData(6, 7);

        // Then
        verify(userRepository).migrateEntityIndicatorsToAnotherUser(6, 7);
    }

    @Test
    void shouldReturnTrueWhenResettingEmployeePassword() {
        // Given
        var user = new User();
        user.setId(8);
        user.setEmail("employee@example.com");
        user.setPreferredUILanguage(new LanguageTag() {{
            setLanguageTag("SR");
        }});
        when(userRepository.findById(8)).thenReturn(Optional.of(user));
        when(messageSource.getMessage(eq("adminPasswordReset.mailSubject"), any(), any()))
            .thenReturn("Reset Password");
        when(messageSource.getMessage(eq("adminPasswordReset.mailBodyEmployee"), any(), any()))
            .thenReturn("New password");

        var emailFuture = CompletableFuture.completedFuture(true);
        when(emailUtil.sendSimpleEmail(any(), any(), any())).thenReturn(emailFuture);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        // When
        var result = userService.generateNewPasswordForUser(8);

        // Then
        assertTrue(result);
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void shouldReturnFalseWhenEmailFailsToSend() {
        // Given
        var user = new User();
        user.setId(9);
        user.setEmail("employee@example.com");
        user.setPreferredUILanguage(new LanguageTag() {{
            setLanguageTag("SR");
        }});
        when(userRepository.findById(9)).thenReturn(Optional.of(user));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("msg");

        var emailFuture = CompletableFuture.completedFuture(false);
        when(emailUtil.sendSimpleEmail(any(), any(), any())).thenReturn(emailFuture);

        // When
        var result = userService.generateNewPasswordForUser(9);

        // Then
        assertFalse(result);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldPerformLogout() {
        // Given
        var jti = "sample-jti-123";

        // When
        userService.logout(jti);

        // Then
        verify(tokenUtil).revokeToken(jti);
        verifyNoMoreInteractions(tokenUtil);
    }

    @Test
    public void shouldConfirmEmailChangeWhenTokenIsValid() {
        // given
        var token = "valid-token";
        var user = new User();
        user.setEmail("old@example.com");

        var emailUpdateRequest = new EmailUpdateRequest();
        emailUpdateRequest.setEmailUpdateToken(token);
        emailUpdateRequest.setNewEmailAddress("new@example.com");
        emailUpdateRequest.setUser(user);

        when(emailUpdateRequestRepository.findByEmailUpdateToken(token))
            .thenReturn(Optional.of(emailUpdateRequest));

        // when
        boolean result = userService.confirmEmailChange(token);

        // then
        assertTrue(result);
        assertEquals("new@example.com", user.getEmail());
        verify(userRepository).save(user);
        verify(emailUpdateRequestRepository).delete(emailUpdateRequest);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsInvalid() {
        // given
        var token = "invalid-token";
        when(emailUpdateRequestRepository.findByEmailUpdateToken(token))
            .thenReturn(Optional.empty());

        // when
        boolean result = userService.confirmEmailChange(token);

        // then
        assertFalse(result);
        verify(userRepository, never()).save(any());
        verify(emailUpdateRequestRepository, never()).delete(any());
    }

    @Test
    void shouldFinishOAuthWorkflowWhenCodeIsValid() {
        // given
        SecurityContextHolder.clearContext();
        var code = "test-code";
        var identifier = "test-identifier";
        var fingerprint = "fp";
        var userId = 123;

        var oauthCodeEntity = new OAuthCode(code, identifier, userId);
        var user = new User();
        user.setId(userId);
        user.setAuthority(new Authority() {{
            setName("ROLE_USER");
        }});

        when(oAuthCodeRepository.getCodeForCodeAndIdentifier(code, identifier))
            .thenReturn(Optional.of(oauthCodeEntity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenUtil.generateToken(any(Authentication.class), eq(fingerprint)))
            .thenReturn("jwt-token");

        // when
        var result = userService.finishOAuthWorkflow(code, identifier, fingerprint);

        // then
        assertEquals("jwt-token", result.getToken());
        assertNotNull(result.getRefreshToken());
        verify(oAuthCodeRepository).deleteByIdentifier(identifier);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldThrowExceptionWhenCodeIsInvalid() {
        // given
        SecurityContextHolder.clearContext();
        var code = "invalid-code";
        var identifier = "test-identifier";

        when(oAuthCodeRepository.getCodeForCodeAndIdentifier(code, identifier))
            .thenReturn(Optional.empty());

        // when / then
        assertThrows(InvalidOAuth2CodeException.class,
            () -> userService.finishOAuthWorkflow(code, identifier, "fp"));

        verify(oAuthCodeRepository).deleteByIdentifier(identifier);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRegisterResearcherOAuthWithExistingPerson() {
        // given
        ReflectionTestUtils.setField(userService, "allowNewResearcherCreation", false);

        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("oauthuser@example.com");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setPersonId(1);
        registrationRequest.setOrganisationUnitId(2);

        var person = new Person();
        person.setName(new PersonName("Jane", "", "Doe", LocalDate.of(1990, 5, 12), null));
        person.setOrcid("orcid-identifier");
        when(personService.findOne(1)).thenReturn(person);

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(anyInt())).thenReturn(organisationUnit);

        var newUser = new User("oauthuser@example.com", "EncodedPassword", "",
            "Jane", "Doe", true, false, language, language, authority,
            person, organisationUnit, null, UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));
        when(userAccountActivationRepository.save(any(UserAccountActivation.class)))
            .thenReturn(new UserAccountActivation(UUID.randomUUID().toString(), newUser));

        // when
        var savedUser = userService.registerResearcherOAuth(
            registrationRequest, OAuth2Provider.ORCID, "orcid-identifier"
        );

        // then
        assertNotNull(savedUser);
        assertEquals("oauthuser@example.com", savedUser.getEmail());
        assertEquals("Jane", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals("orcid-identifier", person.getOrcid());
    }

    @Test
    void shouldRegisterResearcherOAuthWithNewPersonAllowed() {
        // given
        ReflectionTestUtils.setField(userService, "allowNewResearcherCreation", true);

        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("newperson@example.com");
        registrationRequest.setPreferredLanguageId(1);
        registrationRequest.setFirstName("Alice");
        registrationRequest.setLastName("Smith");
        registrationRequest.setOrganisationUnitId(1);

        var person = new Person();
        person.setName(new PersonName("Alice", "", "Smith", null, null));
        person.setOrcid("orcid-id");
        when(personService.createPersonWithBasicInfo(any(BasicPersonDTO.class), eq(true)))
            .thenReturn(person);

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Set.of(new MultiLingualContent(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"),
                "Content", 1)));
        organisationUnit.setIsClientInstitution(true);
        when(organisationUnitService.findOne(1)).thenReturn(organisationUnit);

        var newUser = new User("newperson@example.com", "EncodedPassword", "",
            "Alice", "Smith", true, false, language, language, authority,
            person, null, null, UserNotificationPeriod.NEVER);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        when(brandingInformationService.readBrandingInformation()).thenReturn(
            new BrandingInformationDTO(new ArrayList<>(), new ArrayList<>()));
        when(userAccountActivationRepository.save(any(UserAccountActivation.class)))
            .thenReturn(new UserAccountActivation(UUID.randomUUID().toString(), newUser));

        // when
        var savedUser = userService.registerResearcherOAuth(
            registrationRequest, OAuth2Provider.ORCID, "orcid-id"
        );

        // then
        assertNotNull(savedUser);
        assertEquals("Alice", savedUser.getFirstname());
        assertEquals("Smith", savedUser.getLastName());
        assertEquals("orcid-id", person.getOrcid());
    }

    @Test
    void shouldThrowWhenPersonAlreadyBounded() {
        // given
        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setPersonId(5);

        when(userRepository.personAlreadyBinded(5)).thenReturn(true);

        // when / then
        assertThrows(PersonReferenceConstraintViolationException.class, () ->
            userService.registerResearcherOAuth(registrationRequest, OAuth2Provider.ORCID, "id"));
    }

    @Test
    void shouldThrowWhenCreationNotAllowedAndNoPersonId() {
        // given
        ReflectionTestUtils.setField(userService, "allowNewResearcherCreation", false);

        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("noallowed@example.com");

        // when / then
        assertThrows(PersonReferenceConstraintViolationException.class, () ->
            userService.registerResearcherOAuth(registrationRequest, OAuth2Provider.ORCID, "id"));
    }

    @Test
    void shouldThrowWhenAuthorityNotFound() {
        // given
        when(authorityRepository.findByName(UserRole.RESEARCHER.toString()))
            .thenReturn(Optional.empty());

        var registrationRequest = new ResearcherRegistrationRequestDTO();
        registrationRequest.setEmail("noauth@example.com");
        registrationRequest.setPersonId(1);

        // when / then
        assertThrows(NotFoundException.class, () ->
            userService.registerResearcherOAuth(registrationRequest, OAuth2Provider.ORCID, "id"));
    }
}
