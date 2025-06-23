package rs.teslaris.core.service.impl.user;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import com.google.common.cache.Cache;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.person.UserConverter;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.CommissionRegistrationRequestDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.user.EmailUpdateRequest;
import rs.teslaris.core.model.user.PasswordResetToken;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.EmailUpdateRequestRepository;
import rs.teslaris.core.repository.user.PasswordResetTokenRepository;
import rs.teslaris.core.repository.user.RefreshTokenRepository;
import rs.teslaris.core.repository.user.UserAccountActivationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.PasswordUtil;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PasswordException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Traceable
public class UserServiceImpl extends JPAServiceImpl<User> implements UserService {

    private final MessageSource messageSource;

    private final JwtUtil tokenUtil;

    private final UserRepository userRepository;

    private final UserAccountIndexRepository userAccountIndexRepository;

    private final AuthorityRepository authorityRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserAccountActivationRepository userAccountActivationRepository;

    private final LanguageService languageService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final EmailUtil emailUtil;

    private final PasswordEncoder passwordEncoder;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final SearchService<UserAccountIndex> searchService;

    private final MultilingualContentService multilingualContentService;

    private final Cache<String, Byte> passwordResetRequestCacheStore;

    private final CommissionRepository commissionRepository;

    private final EmailUpdateRequestRepository emailUpdateRequestRepository;

    @Value("${frontend.application.address}")
    private String clientAppAddress;


    @Override
    protected JpaRepository<User, Integer> getEntityRepository() {
        return userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(
            () -> new UsernameNotFoundException("User with this email does not exist."));
    }

    @Override
    public Page<UserAccountIndex> searchUserAccounts(List<String> tokens,
                                                     List<UserRole> allowedRoles,
                                                     Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, allowedRoles),
            pageable, UserAccountIndex.class, "user_account");
    }

    @Deprecated(forRemoval = true)
    public User loadUserById(Integer userID) throws UsernameNotFoundException {
        return userRepository.findById(userID)
            .orElseThrow(() -> new UsernameNotFoundException("User with this ID does not exist."));
    }

    @Override
    @Transactional
    public UserResponseDTO getUserProfile(Integer userId) {
        var user = findOne(userId);
        return UserConverter.toUserResponseDTO(user);
    }

    @Override
    public int getUserOrganisationUnitId(Integer userId) {
        return userRepository.findByIdWithOrganisationUnit(userId)
            .orElseThrow(() -> new NotFoundException("User with this ID does not exist."))
            .getOrganisationUnit().getId();
    }

    @Override
    public int getUserCommissionId(Integer userId) {
        return userRepository.findCommissionIdForUser(userId).orElseThrow(
            () -> new NotFoundException(
                "User with ID " + userId + " either does not exist or is not a commission user."));
    }

    @Override
    @Transactional
    public Integer getPersonIdForUser(Integer userId) {
        var user = findOne(userId);
        if (user.getPerson() == null) {
            return -1;
        }

        return user.getPerson().getId();
    }

    @Override
    @Transactional
    public boolean isUserAResearcher(Integer userId, Integer personId) {
        var user = findOne(userId);

        return user.getPerson() != null && Objects.equals(user.getPerson().getId(), personId);
    }

    @Override
    public AuthenticationResponseDTO authenticateUser(AuthenticationManager authenticationManager,
                                                      AuthenticationRequestDTO authenticationRequest,
                                                      String fingerprint) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(),
                authenticationRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var refreshTokenValue =
            createAndSaveRefreshTokenForUser((User) authentication.getPrincipal());

        return new AuthenticationResponseDTO(tokenUtil.generateToken(authentication, fingerprint),
            refreshTokenValue);
    }

    @Override
    @Transactional
    public AuthenticationResponseDTO refreshToken(String refreshTokenValue, String fingerprint) {
        var hashedRefreshToken =
            Hashing.sha256().hashString(refreshTokenValue, StandardCharsets.UTF_8).toString();

        var refreshToken = refreshTokenRepository.getRefreshToken(hashedRefreshToken).orElseThrow(
            () -> new NonExistingRefreshTokenException("Non existing refresh token provided."));

        var newRefreshToken = createAndSaveRefreshTokenForUser(refreshToken.getUser());
        refreshTokenRepository.delete(refreshToken);

        return new AuthenticationResponseDTO(
            tokenUtil.generateToken(refreshToken.getUser(), fingerprint), newRefreshToken);
    }

    @Override
    public AuthenticationResponseDTO takeRoleOfUser(TakeRoleOfUserRequestDTO takeRoleOfUserRequest,
                                                    String fingerprint) {
        var user = (User) loadUserByUsername(takeRoleOfUserRequest.getUserEmail());

        if (!user.getCanTakeRole()) {
            throw new TakeOfRoleNotPermittedException(
                "User did not allow taking control of his account.");
        }

        tokenUtil.revokeToken(user.getId());
        user.setCanTakeRole(false);
        userRepository.save(user);

        var refreshTokenValue = createAndSaveRefreshTokenForUser(user);

        return new AuthenticationResponseDTO(tokenUtil.generateToken(user, fingerprint),
            refreshTokenValue);
    }

    @Override
    public void allowTakingRoleOfAccount(String bearerToken) {
        var email = tokenUtil.extractUsernameFromToken(bearerToken.split(" ")[1]);

        var user = (User) loadUserByUsername(email);
        user.setCanTakeRole(!user.getCanTakeRole());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Integer userId) {
        var userToDeactivate = findOne(userId);
        if (userToDeactivate.getAuthority().getName().equals("ADMIN")) {
            throw new CantEditException("You can't deactivate an admin user.");
        }

        userToDeactivate.setLocked(!userToDeactivate.getLocked());
        userRepository.save(userToDeactivate);

        updateActivationStatusInUserIndex(userToDeactivate);
        tokenUtil.revokeToken(userId);
    }

    @Override
    @Transactional
    public void activateUserAccount(String activationTokenValue) {
        var accountActivation =
            userAccountActivationRepository.findByActivationToken(activationTokenValue)
                .orElseThrow(() -> new NotFoundException("Invalid activation token"));

        var userToActivate = accountActivation.getUser();
        userToActivate.setLocked(false);

        userRepository.save(userToActivate);
        userAccountActivationRepository.delete(accountActivation);

        updateActivationStatusInUserIndex(userToActivate);
    }

    @Override
    @Transactional
    public User registerResearcher(ResearcherRegistrationRequestDTO registrationRequest) {
        validateEmailUniqueness(registrationRequest.getEmail());
        validatePasswordStrength(registrationRequest.getPassword());

        var authority = authorityRepository.findByName(UserRole.RESEARCHER.toString())
            .orElseThrow(() -> new NotFoundException("Default authority not initialized."));

        Person person;
        if (registrationRequest.getPersonId() != null) {
            if (userRepository.personAlreadyBinded(registrationRequest.getPersonId())) {
                throw new PersonReferenceConstraintViolationException(
                    "Person you have selected is already assigned to a user.");
            }

            person = personService.findOne(registrationRequest.getPersonId());
        } else {
            BasicPersonDTO basicPersonDTO = new BasicPersonDTO();
            PersonNameDTO personNameDTO = new PersonNameDTO();
            personNameDTO.setFirstname(registrationRequest.getFirstName());
            personNameDTO.setLastname(registrationRequest.getLastName());
            personNameDTO.setOtherName("");
            basicPersonDTO.setPersonName(personNameDTO);
            basicPersonDTO.setOrganisationUnitId(registrationRequest.getOrganisationUnitId());

            person = personService.createPersonWithBasicInfo(basicPersonDTO, true);
        }
        var involvement = personService.getLatestResearcherInvolvement(person);

        var newUser =
            new User(registrationRequest.getEmail(),
                passwordEncoder.encode(registrationRequest.getPassword()), "",
                person.getName().getFirstname(), person.getName().getLastname(), true, false,
                languageService.findOne(registrationRequest.getPreferredLanguageId()),
                languageService.findOne(registrationRequest.getPreferredLanguageId()), authority,
                person, Objects.nonNull(involvement) ? involvement.getOrganisationUnit() : null,
                null, UserNotificationPeriod.NEVER);
        var savedUser = userRepository.save(newUser);

        indexUser(savedUser, new UserAccountIndex());

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        userAccountActivationRepository.save(activationToken);

        var language = savedUser.getPreferredUILanguage().getLanguageCode().toLowerCase();
        var activationLink =
            clientAppAddress + (clientAppAddress.endsWith("/") ? language : "/" + language) +
                "/activate-account/" + activationToken.getActivationToken();

        var subject = messageSource.getMessage(
            "accountActivation.mailSubject",
            new Object[] {},
            Locale.forLanguageTag(language)
        );

        var message = messageSource.getMessage(
            "accountActivation.mailBodyResearcher",
            new Object[] {activationLink},
            Locale.forLanguageTag(language)
        );
        emailUtil.sendSimpleEmail(newUser.getEmail(), subject, message);

        return savedUser;
    }

    @Override
    public User registerInstitutionEmployee(EmployeeRegistrationRequestDTO registrationRequest,
                                            UserRole userRole) throws NoSuchAlgorithmException {
        var authorityName = userRole.toString();
        return registerUser(
            registrationRequest.getEmail(),
            registrationRequest.getNote(),
            registrationRequest.getName(),
            registrationRequest.getSurname(),
            registrationRequest.getPreferredLanguageId(),
            registrationRequest.getOrganisationUnitId(),
            null, // No commission for institutional employee
            authorityName
        );
    }

    @Override
    public User registerCommissionUser(CommissionRegistrationRequestDTO registrationRequest)
        throws NoSuchAlgorithmException {
        var authorityName = UserRole.COMMISSION.toString();
        return registerUser(
            registrationRequest.getEmail(),
            registrationRequest.getNote(),
            registrationRequest.getName(),
            "",
            registrationRequest.getPreferredLanguageId(),
            registrationRequest.getOrganisationUnitId(),
            registrationRequest.getCommissionId(),
            authorityName
        );
    }

    private User registerUser(String email, String note, String name, String surname,
                              Integer preferredLanguageId, Integer organisationUnitId,
                              Integer commissionId, String authorityName)
        throws NoSuchAlgorithmException {
        validateEmailUniqueness(email);

        var authority = authorityRepository.findByName(authorityName)
            .orElseThrow(() -> new NotFoundException("Default authority not initialized."));

        var organisationUnit = organisationUnitService.findOne(organisationUnitId);
        var commission =
            (Objects.nonNull(commissionId)) ? commissionRepository.findById(commissionId)
                .orElseThrow(() -> new NotFoundException(
                    "Commission with ID " + commissionId + " does not exist.")) : null;

        var random = SecureRandom.getInstance("SHA1PRNG");
        var generatedPassword = PasswordUtil.generatePassword(12 + random.nextInt(6));

        var newUser = new User(
            email,
            passwordEncoder.encode(new String(generatedPassword)),
            note,
            name.trim(),
            surname.trim(),
            true,
            false,
            languageService.findOne(preferredLanguageId),
            languageService.findOne(preferredLanguageId),
            authority,
            null,
            organisationUnit,
            commission,
            UserNotificationPeriod.NEVER
        );

        var savedUser = userRepository.save(newUser);

        indexUser(savedUser, new UserAccountIndex());

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        userAccountActivationRepository.save(activationToken);

        var language = savedUser.getPreferredUILanguage().getLanguageCode().toLowerCase();
        String activationLink =
            generateActivationLink(language, activationToken.getActivationToken());

        var subject = messageSource.getMessage(
            "accountActivation.mailSubject",
            new Object[] {},
            Locale.forLanguageTag(language)
        );

        var message = messageSource.getMessage(
            "accountActivation.mailBodyEmployee",
            new Object[] {activationLink, new String(generatedPassword)},
            Locale.forLanguageTag(language)
        );

        emailUtil.sendSimpleEmail(newUser.getEmail(), subject, message);

        Arrays.fill(generatedPassword, '\0');
        return savedUser;
    }

    private String generateActivationLink(String language, String activationToken) {
        return clientAppAddress + (clientAppAddress.endsWith("/") ? language : "/" + language) +
            "/activate-account/" + activationToken;
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("emailInUseMessage");
        }
    }

    private void validatePasswordStrength(String password) {
        if (!PasswordUtil.validatePasswordStrength(password)) {
            throw new PasswordException(
                "Weak password, use at least 8 characters, one upper and one lower case, and a number.");
        }
    }

    @Override
    @Transactional
    public AuthenticationResponseDTO updateUser(UserUpdateRequestDTO userUpdateRequest,
                                                Integer userId, String fingerprint) {
        var userToUpdate = findOne(userId);
        var userRole = userToUpdate.getAuthority().getName();

        updateNameAndOrgIfAllowed(userUpdateRequest, userToUpdate, userRole);

        updatePreferredLanguages(userToUpdate, userUpdateRequest);

        if (emailChanged(userToUpdate, userUpdateRequest)) {
            createAndSendEmailUpdateRequest(userToUpdate, userUpdateRequest.getEmail());
        }

        userToUpdate.setUserNotificationPeriod(userUpdateRequest.getNotificationPeriod());

        validateNotificationSettings(userToUpdate);

        handlePasswordChange(userToUpdate, userUpdateRequest);

        userRepository.save(userToUpdate);

        updateUserIndex(userToUpdate);
        tokenUtil.revokeToken(userToUpdate.getId());

        var refreshTokenValue = createAndSaveRefreshTokenForUser(userToUpdate);
        return new AuthenticationResponseDTO(
            tokenUtil.generateToken(userToUpdate, fingerprint),
            refreshTokenValue
        );
    }

    private void updateNameAndOrgIfAllowed(UserUpdateRequestDTO dto, User user, String role) {
        if (role.equals(UserRole.INSTITUTIONAL_EDITOR.toString())) {
            user.setFirstname(dto.getFirstname());
            user.setLastName(dto.getLastName());
            var orgUnit = organisationUnitService.findOne(dto.getOrganisationalUnitId());
            user.setOrganisationUnit(orgUnit);
        } else if (Set.of(
            UserRole.ADMIN.toString(),
            UserRole.COMMISSION.toString(),
            UserRole.VICE_DEAN_FOR_SCIENCE.toString(),
            UserRole.INSTITUTIONAL_LIBRARIAN.toString(),
            UserRole.HEAD_OF_LIBRARY.toString(),
            UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString()
        ).contains(role)) {
            user.setFirstname(dto.getFirstname());
            user.setLastName(dto.getLastName());
        }
    }

    private void updatePreferredLanguages(User user, UserUpdateRequestDTO dto) {
        var uiLang = languageService.findOne(dto.getPreferredUILanguageId());
        var refLang = languageService.findOne(dto.getPreferredReferenceCataloguingLanguageId());
        user.setPreferredUILanguage(uiLang);
        user.setPreferredReferenceCataloguingLanguage(refLang);
    }

    private boolean emailChanged(User user, UserUpdateRequestDTO dto) {
        return !user.getEmail().equals(dto.getEmail());
    }

    private void createAndSendEmailUpdateRequest(User user, String newEmail) {
        var emailUpdateRequest = new EmailUpdateRequest();
        emailUpdateRequest.setEmailUpdateToken(UUID.randomUUID().toString());
        emailUpdateRequest.setNewEmailAddress(newEmail);
        emailUpdateRequest.setUser(user);
        emailUpdateRequestRepository.save(emailUpdateRequest);

        var languageCode = user.getPreferredUILanguage().getLanguageCode().toLowerCase();
        var confirmationLink = clientAppAddress +
            (clientAppAddress.endsWith("/") ? languageCode : "/" + languageCode) +
            "/update-email/" + emailUpdateRequest.getEmailUpdateToken();

        var subject = messageSource.getMessage("emailUpdateRequest.mailSubject", null,
            Locale.forLanguageTag(languageCode));
        var message = messageSource.getMessage("emailUpdateRequest.mailBody",
            new Object[] {confirmationLink}, Locale.forLanguageTag(languageCode));

        emailUtil.sendSimpleEmail(user.getEmail(), subject, message);
    }

    private void validateNotificationSettings(User user) {
        if (!user.getUserNotificationPeriod().equals(UserNotificationPeriod.NEVER) &&
            user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                "You have to setup username before you can receive email notifications.");
        }
    }

    private void handlePasswordChange(User user, UserUpdateRequestDTO dto) {
        if (!dto.getOldPassword().isEmpty()) {
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new PasswordException("wrongOldPasswordError");
            }
            if (!PasswordUtil.validatePasswordStrength(dto.getNewPassword())) {
                throw new PasswordException("weakPasswordError");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
    }

    private void updateUserIndex(User user) {
        var index = userAccountIndexRepository.findByDatabaseId(user.getId())
            .orElse(new UserAccountIndex());
        indexUser(user, index);
    }

    @Override
    @Transactional
    public boolean confirmEmailChange(String emailUpdateToken) {
        var emailUpdateRequest =
            emailUpdateRequestRepository.findByEmailUpdateToken(emailUpdateToken);

        if (emailUpdateRequest.isPresent()) {
            emailUpdateRequest.get().getUser()
                .setEmail(emailUpdateRequest.get().getNewEmailAddress());
            userRepository.save(emailUpdateRequest.get().getUser());
            emailUpdateRequestRepository.delete(emailUpdateRequest.get());
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void initiatePasswordResetProcess(ForgotPasswordRequestDTO forgotPasswordRequest) {
        String userEmail = forgotPasswordRequest.getUserEmail();

        if (passwordResetRequestCacheStore.getIfPresent(userEmail) != null) {
            return;
        }

        passwordResetRequestCacheStore.put(userEmail, (byte) 1);

        try {
            var user = (User) loadUserByUsername(userEmail);
            tokenUtil.revokeToken(user.getId());
            var resetToken = UUID.randomUUID().toString();
            var language = user.getPreferredUILanguage().getLanguageCode().toLowerCase();

            String resetLink =
                clientAppAddress + (clientAppAddress.endsWith("/") ? language : "/" + language) +
                    "/reset-password/" + resetToken;
            String emailSubject = messageSource.getMessage(
                "resetPassword.mailSubject",
                new Object[] {},
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageCode().toLowerCase())
            );
            String emailBody = messageSource.getMessage(
                "resetPassword.mailBody",
                new Object[] {resetLink},
                Locale.forLanguageTag(
                    user.getPreferredUILanguage().getLanguageCode().toLowerCase())
            );

            emailUtil.sendSimpleEmail(user.getEmail(), emailSubject, emailBody);
            passwordResetTokenRepository.save(new PasswordResetToken(resetToken, user));
        } catch (UsernameNotFoundException ignored) {
        }
    }

    @Override
    @Transactional
    public void resetAccountPassword(ResetPasswordRequestDTO resetPasswordRequest) {
        var resetRequest = passwordResetTokenRepository.findByPasswordResetToken(
                resetPasswordRequest.getResetToken())
            .orElseThrow(() -> new NotFoundException("Invalid password reset token"));

        resetRequest.getUser().setPassword(passwordEncoder.encode(
            resetPasswordRequest.getNewPassword()));
        resetRequest.getUser().setLocked(false);

        userRepository.save(resetRequest.getUser());
        passwordResetTokenRepository.delete(resetRequest);
        updateActivationStatusInUserIndex(resetRequest.getUser());
    }

    @Override
    public void updateResearcherCurrentOrganisationUnitIfBound(Integer personId) {
        var person = personService.findOne(personId);
        var boundUser = userRepository.findForResearcher(personId);

        if (boundUser.isEmpty()) {
            return;
        }

        var latestInvolvement = personService.getLatestResearcherInvolvement(person);

        var userToUpdate = boundUser.get();
        userToUpdate.setOrganisationUnit(
            Objects.nonNull(latestInvolvement) ? latestInvolvement.getOrganisationUnit() : null);
        userRepository.save(userToUpdate);
        var index = userAccountIndexRepository.findByDatabaseId(userToUpdate.getId())
            .orElse(new UserAccountIndex());
        indexUser(userToUpdate, index);
    }

    @Override
    @Transactional
    public UserResponseDTO getUserFromPerson(Integer personId) {
        var boundUser = userRepository.findForResearcher(personId)
            .orElseThrow(() -> new NotFoundException("personNotBound"));

        return UserConverter.toUserResponseDTO(boundUser);
    }

    @Override
    public List<Integer> getAccountsWithRoleTakingAllowed() {
        return userRepository.getIdsOfUsersWhoAllowedAccountTakeover();
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexUsers() {
        userAccountIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<User> chunk = findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((user) -> indexUser(user, new UserAccountIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
        return null;
    }

    @Override
    public List<Commission> findCommissionForOrganisationUnitId(Integer organisationUnitId) {
        return userRepository.findUserCommissionForOrganisationUnit(organisationUnitId);
    }

    @Override
    public List<User> findAllCommissionUsers() {
        return userRepository.findAllCommissionUsers();
    }

    @Override
    public List<User> findAllSystemAdminUsers() {
        return userRepository.findAllSystemAdminUsers();
    }

    @Override
    @Transactional
    public void deleteUserAccount(Integer userId) {
        var userToDelete = findOne(userId);

        if (userToDelete.getAuthority().getName().equals(UserRole.ADMIN.name())) {
            throw new CantEditException("You can't delete an admin user.");
        }

        if (userRepository.hasUserAssignedIndicators(userId)) {
            throw new ReferenceConstraintException("userHasAssignedIndicatorsMessage");
        }

        tokenUtil.revokeToken(userId);
        userRepository.deleteAllNotificationsForUser(userId);
        userRepository.deleteAllAccountActivationsForUser(userId);
        userRepository.deleteAllPasswordResetsForUser(userId);
        userRepository.deleteRefreshTokenForUser(userId);

        userToDelete.setPerson(null);
        userRepository.delete(userToDelete);
        userAccountIndexRepository.findByDatabaseId(userId)
            .ifPresent(userAccountIndexRepository::delete);
    }

    @Override
    @Transactional
    public void migrateUserAccountData(Integer userToUpdateId,
                                       Integer userToDeleteId) {
        if (Objects.equals(userToUpdateId, userToDeleteId)) {
            throw new ReferenceConstraintException("Can't migrate data to same user.");
        }

        var userToUpdate = findOne(userToUpdateId);
        var userToDelete = findOne(userToDeleteId);

        if ((!userToUpdate.getAuthority().getName().equals(UserRole.COMMISSION.name()) &&
            !userToUpdate.getAuthority().getName().equals(UserRole.RESEARCHER.name())) ||
            !userToUpdate.getAuthority().getName().equals(userToDelete.getAuthority().getName())) {
            throw new ReferenceConstraintException(
                "Only applicable for commission and researcher users.");
        }

        userRepository.migrateEntityIndicatorsToAnotherUser(userToUpdateId, userToDeleteId);
    }

    @Override
    @Transactional
    public boolean generateNewPasswordForUser(Integer userId) {
        var savedUser = findOne(userId);
        tokenUtil.revokeToken(userId);

        SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // should never happen
        }

        var generatedPassword = PasswordUtil.generatePassword(12 + random.nextInt(6));

        var language = savedUser.getPreferredUILanguage().getLanguageCode().toLowerCase();

        var subject = messageSource.getMessage(
            "adminPasswordReset.mailSubject",
            new Object[] {},
            Locale.forLanguageTag(language)
        );

        var message = messageSource.getMessage(
            "adminPasswordReset.mailBodyEmployee",
            new Object[] {new String(generatedPassword)},
            Locale.forLanguageTag(language)
        );

        var emailIsSent = emailUtil.sendSimpleEmail(savedUser.getEmail(), subject, message);
        try {
            if (emailIsSent.get()) {
                savedUser.setPassword(passwordEncoder.encode(new String(generatedPassword)));
                return true;
            }
        } catch (InterruptedException | ExecutionException ignored) {
            // handled in the code block below
        }

        Arrays.fill(generatedPassword, '\0');
        return false;
    }

    @Override
    public void logout(String jti) {
        tokenUtil.revokeToken(jti);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private String createAndSaveRefreshTokenForUser(User user) {
        var refreshTokenValue = UUID.randomUUID().toString();
        var hashedRefreshToken =
            Hashing.sha256().hashString(refreshTokenValue, StandardCharsets.UTF_8).toString();

        var oldRefreshToken = refreshTokenRepository.findByUserId(user.getId());
        oldRefreshToken.ifPresent(refreshTokenRepository::delete);

        refreshTokenRepository.save(new RefreshToken(hashedRefreshToken, user));

        return refreshTokenValue;
    }

    private void indexUserEmployment(UserAccountIndex index, OrganisationUnit employment) {
        var orgUnitNameSr = new StringBuilder();
        var orgUnitNameOther = new StringBuilder();
        if (Objects.nonNull(employment)) {
            multilingualContentService.buildLanguageStrings(orgUnitNameSr, orgUnitNameOther,
                employment.getName(), true);
        }

        StringUtil.removeTrailingDelimiters(orgUnitNameSr, orgUnitNameOther);
        index.setOrganisationUnitNameSr(
            orgUnitNameSr.length() > 0 ? orgUnitNameSr.toString() : orgUnitNameOther.toString());
        index.setOrganisationUnitNameOther(
            orgUnitNameOther.length() > 0 ? orgUnitNameOther.toString() : orgUnitNameSr.toString());
    }

    private void indexUser(User user, UserAccountIndex index) {
        index.setDatabaseId(user.getId());

        indexCommonFields(index, user);

        userAccountIndexRepository.save(index);
    }

    private void indexCommonFields(UserAccountIndex index, User user) {
        index.setFullName(user.getFirstname() + " " + user.getLastName());
        index.setFullNameSortable(index.getFullName());
        index.setEmail(user.getEmail());
        index.setEmailSortable(user.getEmail());
        index.setUserRole(user.getAuthority().getName());
        index.setActive(!user.getLocked());
        indexUserEmployment(index, user.getOrganisationUnit());
        index.setOrganisationUnitNameSortableSr(index.getOrganisationUnitNameSr());
        index.setOrganisationUnitNameSortableOther(index.getOrganisationUnitNameOther());
    }

    private Query buildSimpleSearchQuery(List<String> tokens, List<UserRole> allowedRoles) {
        return BoolQuery.of(q -> {
            var mustClauses = new ArrayList<Query>();

            for (String token : tokens) {
                var cleanedToken = token.replace("\\\"", "");
                var perTokenShould = new ArrayList<Query>();

                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    perTokenShould.add(MatchPhraseQuery.of(
                            mq -> mq.field("org_unit_name_sr").query(cleanedToken))
                        ._toQuery());
                    perTokenShould.add(MatchPhraseQuery.of(
                            mq -> mq.field("org_unit_name_other").query(cleanedToken))
                        ._toQuery());
                } else {
                    if (token.endsWith("\\*") || token.endsWith(".")) {
                        var wildcard = token.replace("\\*", "").replace(".", "");
                        perTokenShould.add(WildcardQuery.of(
                                m -> m.field("full_name")
                                    .value(StringUtil.performSimpleSerbianPreprocessing(wildcard) + "*")
                                    .caseInsensitive(true))
                            ._toQuery());
                    } else {
                        perTokenShould.add(WildcardQuery.of(
                                m -> m.field("full_name")
                                    .value(StringUtil.performSimpleSerbianPreprocessing(token) + "*")
                                    .caseInsensitive(true))
                            ._toQuery());
                    }

                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("email").query(token).boost(0.7f))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("org_unit_name_sr").query(token).boost(0.5f))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("org_unit_name_other").query(token).boost(0.5f))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("user_role").query(token))._toQuery());
                }

                mustClauses.add(BoolQuery.of(b -> b.should(perTokenShould))._toQuery());
            }

            // Add allowedRoles as a filter
            if (Objects.nonNull(allowedRoles) && !allowedRoles.isEmpty()) {
                mustClauses.add(TermsQuery.of(t -> t
                    .field("user_role")
                    .terms(v -> v.value(
                        allowedRoles.stream()
                            .map(String::valueOf)
                            .map(FieldValue::of)
                            .toList()))
                )._toQuery());
            }

            return q.must(mustClauses);
        })._toQuery();
    }

    private void updateActivationStatusInUserIndex(User user) {
        userAccountIndexRepository.findByDatabaseId(user.getId()).ifPresent(index -> {
            index.setActive(!user.getLocked());
            userAccountIndexRepository.save(index);
        });
    }

    @Scheduled(cron = "0 */10 * ? * *") // every ten minutes
    public void cleanupLongLivedRefreshTokens() {
        var refreshTokens = refreshTokenRepository.findAll();

        var now = new Date();
        var twentyMinutesAgo = new Date(now.getTime() - (20 * 60 * 1000));

        refreshTokens.stream().filter(token -> token.getCreateDate().before(twentyMinutesAgo))
            .forEach(refreshTokenRepository::delete);
    }

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    public void cleanupLongLivedAccountActivationTokens() {
        var activationTokens = userAccountActivationRepository.findAll();

        var now = new Date();
        var sevenDaysAgo = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));

        activationTokens.stream().filter(token -> token.getCreateDate().before(sevenDaysAgo))
            .forEach(userAccountActivationRepository::delete);
    }

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    public void cleanupLongLivedPasswordResetTokens() {
        var activationTokens = passwordResetTokenRepository.findAll();

        var now = new Date();
        var sevenDaysAgo = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));

        activationTokens.stream().filter(token -> token.getCreateDate().before(sevenDaysAgo))
            .forEach(passwordResetTokenRepository::delete);
    }

    @Scheduled(cron = "0 * * * * *") // every 15 minutes
    public void cleanupExpiredTokens() {
        tokenUtil.cleanupExpiredTokens();
    }
}
