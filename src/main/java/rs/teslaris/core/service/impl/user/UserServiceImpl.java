package rs.teslaris.core.service.impl.user;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
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
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.PasswordUtil;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PasswordException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends JPAServiceImpl<User> implements UserService {

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
    public Page<UserAccountIndex> searchUserAccounts(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens),
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
        var organisationUnitId =
            Objects.nonNull(user.getOrganisationUnit()) ? user.getOrganisationUnit().getId() : -1;
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getFirstname(),
            user.getLastName(), user.getLocked(), user.getCanTakeRole(),
            user.getPreferredLanguage().getLanguageCode(), organisationUnitId);
    }

    @Override
    public int getUserOrganisationUnitId(Integer userId) {
        return userRepository.findByIdWithOrganisationUnit(userId)
            .orElseThrow(() -> new NotFoundException("User with this ID does not exist."))
            .getOrganisationUnit().getId();
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
            return;
        }

        userToDeactivate.setLocked(!userToDeactivate.getLocked());
        userRepository.save(userToDeactivate);

        var index = userAccountIndexRepository.findByDatabaseId(userId);
        if (index.isPresent()) {
            index.get().setActive(!userToDeactivate.getLocked());
            userAccountIndexRepository.save(index.get());
        }
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

        var index = userAccountIndexRepository.findByDatabaseId(userToActivate.getId());
        if (index.isPresent()) {
            index.get().setActive(!userToActivate.getLocked());
            userAccountIndexRepository.save(index.get());
        }
    }

    @Override
    @Transactional
    public User registerResearcher(ResearcherRegistrationRequestDTO registrationRequest) {
        validateEmailUniqueness(registrationRequest.getEmail());
        validatePasswordStrength(registrationRequest.getPassword());

        var authority = authorityRepository.findByName(UserRole.RESEARCHER.toString())
            .orElseThrow(() -> new NotFoundException("Default authority not initialized."));

        var person = personService.findOne(registrationRequest.getPersonId());
        var organisationUnit = getLatestResearcherInvolvement(person);

        var newUser =
            new User(registrationRequest.getEmail(),
                passwordEncoder.encode(registrationRequest.getPassword()), "",
                person.getName().getFirstname(), person.getName().getLastname(), true, false,
                languageService.findOne(registrationRequest.getPreferredLanguageId()), authority,
                person, organisationUnit);
        var savedUser = userRepository.save(newUser);

        reindexUser(savedUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        userAccountActivationRepository.save(activationToken);

        // Email message should be localised and customized
        emailUtil.sendSimpleEmail(newUser.getEmail(), "Account activation",
            "Your activation code is: " + activationToken.getActivationToken());

        return savedUser;
    }

    @Override
    public User registerInstitutionAdmin(EmployeeRegistrationRequestDTO registrationRequest) {
        validateEmailUniqueness(registrationRequest.getEmail());

        var authority = authorityRepository.findByName(UserRole.INSTITUTIONAL_EDITOR.toString())
            .orElseThrow(() -> new NotFoundException("Default authority not initialized."));

        var organisationUnit =
            organisationUnitService.findOne(registrationRequest.getOrganisationUnitId());

        var generatedPassword =
            PasswordUtil.generatePassword(12 + (int) (Math.random() * ((18 - 12) + 1)));

        var newUser =
            new User(registrationRequest.getEmail(),
                passwordEncoder.encode(new String(generatedPassword)),
                registrationRequest.getNote(),
                registrationRequest.getName(), registrationRequest.getSurname(), true, false,
                languageService.findOne(registrationRequest.getPreferredLanguageId()), authority,
                null, organisationUnit);
        var savedUser = userRepository.save(newUser);

        reindexUser(savedUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        userAccountActivationRepository.save(activationToken);

        // Email message should be localised and customized
        emailUtil.sendSimpleEmail(newUser.getEmail(), "Account activation",
            "Your activation code is: " + activationToken.getActivationToken() +
                "\n\nYour password is: " + new String(generatedPassword));

        Arrays.fill(generatedPassword, '\0');
        return savedUser;
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email " + email + " is already in use!");
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

        var preferredLanguage = languageService.findOne(userUpdateRequest.getPreferredLanguageId());

        if (userToUpdate.getAuthority().getName()
            .equals(UserRole.INSTITUTIONAL_EDITOR.toString())) {
            userToUpdate.setFirstname(userUpdateRequest.getFirstname());
            userToUpdate.setLastName(userUpdateRequest.getLastName());
            var orgUnit =
                organisationUnitService.findOne(userUpdateRequest.getOrganisationalUnitId());
            userToUpdate.setOrganisationUnit(orgUnit);
        }

        userToUpdate.setEmail(userUpdateRequest.getEmail());
        // TODO: Maybe add email verification?
        userToUpdate.setPreferredLanguage(preferredLanguage);

        if (!userUpdateRequest.getOldPassword().isEmpty() &&
            passwordEncoder.matches(userUpdateRequest.getOldPassword(),
                userToUpdate.getPassword())) {

            if (!PasswordUtil.validatePasswordStrength(userUpdateRequest.getNewPassword())) {
                throw new PasswordException(
                    "Weak password, use at least 8 characters, one upper and one lower case and a number.");
            }

            userToUpdate.setPassword(passwordEncoder.encode(userUpdateRequest.getNewPassword()));
        } else if (!userUpdateRequest.getOldPassword().isEmpty()) {
            throw new PasswordException("Wrong old password provided.");
        }

        userRepository.save(userToUpdate);
        reindexUser(userToUpdate);

        var refreshTokenValue = createAndSaveRefreshTokenForUser(userToUpdate);
        return new AuthenticationResponseDTO(tokenUtil.generateToken(userToUpdate, fingerprint),
            refreshTokenValue);
    }

    @Override
    @Transactional
    public void initiatePasswordResetProcess(ForgotPasswordRequestDTO forgotPasswordRequest) {
        var user = (User) loadUserByUsername(forgotPasswordRequest.getUserEmail());

        var resetToken = UUID.randomUUID().toString();
        emailUtil.sendSimpleEmail(user.getEmail(), "Account activation",
            "To reset your password, go to: <BASE_URL>" + resetToken +
                "\n\nThis token will last a week.");

        passwordResetTokenRepository.save(new PasswordResetToken(resetToken, user));
    }

    @Override
    @Transactional
    public void resetAccountPassword(ResetPasswordRequestDTO resetPasswordRequest) {
        var resetRequest = passwordResetTokenRepository.findByPasswordResetToken(
                resetPasswordRequest.getResetToken())
            .orElseThrow(() -> new NotFoundException("Invalid password reset token"));

        resetRequest.getUser().setPassword(passwordEncoder.encode(
            resetPasswordRequest.getNewPassword()));

        userRepository.save(resetRequest.getUser());
        passwordResetTokenRepository.delete(resetRequest);
    }

    @Override
    public void updateResearcherCurrentOrganisationUnitIfBound(Integer personId) {
        var person = personService.findOne(personId);
        var boundUser = userRepository.findForResearcher(personId);

        if (boundUser.isEmpty()) {
            return;
        }

        var userToUpdate = boundUser.get();
        userToUpdate.setOrganisationUnit(getLatestResearcherInvolvement(person));
        userRepository.save(userToUpdate);
        reindexUser(userToUpdate);
    }

    private OrganisationUnit getLatestResearcherInvolvement(Person person) {
        OrganisationUnit organisationUnit = null;
        if (Objects.nonNull(person.getInvolvements())) {
            // TODO: Ulazi li ovo samo u display OU ili i edukacija
            Optional<Involvement> latestInvolvement = person.getInvolvements().stream()
                .filter(involvement -> Objects.nonNull(involvement.getOrganisationUnit()))
                .filter(involvement ->
                    involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                        involvement.getInvolvementType().equals(InvolvementType.HIRED_BY))
                .max(Comparator.comparing(Involvement::getDateFrom));

            if (latestInvolvement.isPresent()) {
                organisationUnit = latestInvolvement.get().getOrganisationUnit();
            }
        }

        return organisationUnit;
    }

    private String createAndSaveRefreshTokenForUser(User user) {
        var refreshTokenValue = UUID.randomUUID().toString();
        var hashedRefreshToken =
            Hashing.sha256().hashString(refreshTokenValue, StandardCharsets.UTF_8).toString();
        refreshTokenRepository.save(new RefreshToken(hashedRefreshToken, user));

        return refreshTokenValue;
    }

    private void indexUserEmployment(UserAccountIndex index, OrganisationUnit employment) {
        var orgUnitNameSr = new StringBuilder();
        var orgUnitNameOther = new StringBuilder();
        if (Objects.nonNull(employment)) {
            employment.getName().forEach((name) -> {
                if (name.getLanguage().getLanguageTag().contains(LanguageAbbreviations.SERBIAN)) {
                    orgUnitNameSr.append(name.getContent()).append(" | ");
                } else {
                    orgUnitNameOther.append(name.getContent()).append(" | ");
                }
            });
        }

        StringUtil.removeTrailingPipeDelimiter(orgUnitNameSr, orgUnitNameOther);
        index.setOrganisationUnitNameSr(
            orgUnitNameSr.length() > 0 ? orgUnitNameSr.toString() : orgUnitNameOther.toString());
        index.setOrganisationUnitNameOther(
            orgUnitNameOther.length() > 0 ? orgUnitNameOther.toString() : orgUnitNameSr.toString());
    }

    private void reindexUser(User user) {
        var index = userAccountIndexRepository.findByDatabaseId(user.getId())
            .orElse(new UserAccountIndex());
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

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.wildcard(
                    m -> m.field("full_name").value(token).caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("email").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("org_unit_name_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("org_unit_name_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("user_role").query(token)));
            });
            return b;
        })))._toQuery();
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
}
