package rs.teslaris.core.service.impl.user;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.dto.user.ForgotPasswordSubmissionDTO;
import rs.teslaris.core.dto.user.RegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.model.user.UserPasswordResetRequest;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.RefreshTokenRepository;
import rs.teslaris.core.repository.user.UserAccountActivationRepository;
import rs.teslaris.core.repository.user.UserPasswordResetRequestRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.WrongPasswordProvidedException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends JPAServiceImpl<User> implements UserService {

    private final JwtUtil tokenUtil;

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserAccountActivationRepository userAccountActivationRepository;

    private final LanguageService languageService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final EmailUtil emailUtil;

    private final PasswordEncoder passwordEncoder;

    private final UserPasswordResetRequestRepository userPasswordResetRequestRepository;

    @Override
    protected JpaRepository<User, Integer> getEntityRepository() {
        return userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(
            () -> new UsernameNotFoundException("User with this email does not exist."));
    }

    @Deprecated(forRemoval = true)
    public User loadUserById(Integer userID) throws UsernameNotFoundException {
        return userRepository.findById(userID)
            .orElseThrow(() -> new UsernameNotFoundException("User with this ID does not exist."));
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
    }

    @Override
    @Transactional
    public User registerUser(RegistrationRequestDTO registrationRequest) {
        var preferredLanguage =
            languageService.findOne(registrationRequest.getPreferredLanguageId());

        var authority = authorityRepository.findByName(UserRole.RESEARCHER.toString())
            .orElseThrow(() -> new NotFoundException("Default authority not initialized."));

        Person person = null;
        if (Objects.nonNull(registrationRequest.getPersonId())) {
            person = personService.findOne(registrationRequest.getPersonId());
        }

        var organisationalUnit = organisationUnitService.findOrganisationUnitById(
            registrationRequest.getOrganisationalUnitId());

        var newUser =
            new User(registrationRequest.getEmail(), registrationRequest.getPassword(), "",
                registrationRequest.getFirstname(), registrationRequest.getLastName(), true, false,
                preferredLanguage, authority, person, organisationalUnit);

        var savedUser = userRepository.save(newUser);

        var activationToken = new UserAccountActivation(UUID.randomUUID().toString(), newUser);
        userAccountActivationRepository.save(activationToken);

        // Email message should be localised and customized
        emailUtil.sendSimpleEmail(newUser.getEmail(), "Account activation",
            "Your activation code is: " + activationToken.getActivationToken());

        return savedUser;
    }

    @Override
    @Transactional
    public AuthenticationResponseDTO updateUser(UserUpdateRequestDTO userUpdateRequest,
                                                Integer userId, String fingerprint) {
        var userToUpdate = findOne(userId);

        var preferredLanguage = languageService.findOne(userUpdateRequest.getPreferredLanguageId());

        var person = personService.findOne(userUpdateRequest.getPersonId());

        var organisationalUnit = organisationUnitService.findOrganisationUnitById(
            userUpdateRequest.getOrganisationalUnitId());

        userToUpdate.setEmail(userUpdateRequest.getEmail());
        userToUpdate.setFirstname(userUpdateRequest.getFirstname());
        userToUpdate.setLastName(userUpdateRequest.getLastName());
        userToUpdate.setPreferredLanguage(preferredLanguage);
        userToUpdate.setPerson(person);
        userToUpdate.setOrganisationUnit(organisationalUnit);

        if (!userUpdateRequest.getOldPassword().equals("") &&
            passwordEncoder.matches(userUpdateRequest.getOldPassword(),
                userToUpdate.getPassword())) {
            userToUpdate.setPassword(passwordEncoder.encode(userUpdateRequest.getNewPassword()));
        } else if (!userUpdateRequest.getOldPassword().equals("")) {
            throw new WrongPasswordProvidedException("Wrong old password provided.");
        }

        userRepository.save(userToUpdate);
        var refreshTokenValue = createAndSaveRefreshTokenForUser(userToUpdate);
        return new AuthenticationResponseDTO(tokenUtil.generateToken(userToUpdate, fingerprint),
            refreshTokenValue);
    }

    @Override
    @Transactional
    public void submitForgottenPassword(ForgotPasswordSubmissionDTO forgotPasswordSubmission) {
        var user = (User) loadUserByUsername(forgotPasswordSubmission.getUserEmail());

        var resetToken = UUID.randomUUID().toString();
        emailUtil.sendSimpleEmail(user.getEmail(), "Account activation",
            "To reset your password, go to: <BASE_URL>" + resetToken);

        userPasswordResetRequestRepository.save(new UserPasswordResetRequest(resetToken, user));
    }

    @Override
    @Transactional
    public void resetAccountPassword(ResetPasswordRequestDTO resetPasswordRequest) {
        var resetRequest = userPasswordResetRequestRepository.findByPasswordResetToken(
                resetPasswordRequest.getResetToken())
            .orElseThrow(() -> new NotFoundException("Invalid password reset token"));

        resetRequest.getUser().setPassword(passwordEncoder.encode(
            resetPasswordRequest.getNewPassword()));

        userRepository.save(resetRequest.getUser());
        userPasswordResetRequestRepository.delete(resetRequest);
    }

    private String createAndSaveRefreshTokenForUser(User user) {
        var refreshTokenValue = UUID.randomUUID().toString();
        var hashedRefreshToken =
            Hashing.sha256().hashString(refreshTokenValue, StandardCharsets.UTF_8).toString();
        refreshTokenRepository.save(new RefreshToken(hashedRefreshToken, user));

        return refreshTokenValue;
    }

    @Scheduled(cron = "0 */5 * ? * *") // every five minutes
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
}
