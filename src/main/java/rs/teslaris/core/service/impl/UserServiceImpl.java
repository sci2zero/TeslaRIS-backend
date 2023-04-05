package rs.teslaris.core.service.impl;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.AuthenticationResponseDTO;
import rs.teslaris.core.dto.RegistrationRequestDTO;
import rs.teslaris.core.dto.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.UserUpdateRequestDTO;
import rs.teslaris.core.exception.CantRegisterAdminException;
import rs.teslaris.core.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.exception.WrongPasswordProvidedException;
import rs.teslaris.core.model.RefreshToken;
import rs.teslaris.core.model.User;
import rs.teslaris.core.model.UserAccountActivation;
import rs.teslaris.core.repository.AuthorityRepository;
import rs.teslaris.core.repository.RefreshTokenRepository;
import rs.teslaris.core.repository.UserAccountActivationRepository;
import rs.teslaris.core.repository.UserRepository;
import rs.teslaris.core.service.LanguageService;
import rs.teslaris.core.service.OrganisationalUnitService;
import rs.teslaris.core.service.PersonService;
import rs.teslaris.core.service.UserService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.jwt.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JwtUtil tokenUtil;

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserAccountActivationRepository userAccountActivationRepository;

    private final LanguageService languageService;

    private final PersonService personService;

    private final OrganisationalUnitService organisationalUnitService;

    private final EmailUtil emailUtil;

    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(
            () -> new UsernameNotFoundException("User with this email does not exist."));
    }

    @Override
    public AuthenticationResponseDTO authenticateUser(AuthenticationManager authenticationManager,
                                                      AuthenticationRequestDTO authenticationRequest,
                                                      String fingerprint) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(),
                authenticationRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var refreshTokenValue =
            createAndSaveRefreshTokenForUser((User) authentication.getPrincipal());

        return new AuthenticationResponseDTO(tokenUtil.generateToken(authentication, fingerprint),
            refreshTokenValue);
    }

    @Override
    public AuthenticationResponseDTO refreshToken(String refreshTokenValue, String fingerprint) {
        var hashedRefreshToken = Hashing.sha256()
            .hashString(refreshTokenValue, StandardCharsets.UTF_8)
            .toString();

        var refreshToken = refreshTokenRepository.getRefreshToken(hashedRefreshToken).orElseThrow(
            () -> new NonExistingRefreshTokenException("Non existing refresh token provided."));

        var newRefreshToken = createAndSaveRefreshTokenForUser(refreshToken.getUser());
        refreshTokenRepository.delete(refreshToken);

        return new AuthenticationResponseDTO(
            tokenUtil.generateToken(refreshToken.getUser(), fingerprint),
            newRefreshToken);
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
        user.setCanTakeRole(true);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Integer userId) {
        var userToDeactivate = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with given ID does not exist."));

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
            languageService.findLanguageById(registrationRequest.getPreferredLanguageId());

        var authority = authorityRepository.findById(registrationRequest.getAuthorityId())
            .orElseThrow(() -> new NotFoundException("Authority with given ID does not exist."));

        if (authority.getName().equals("ADMIN")) {
            throw new CantRegisterAdminException("Can't register new admin.");
        }

        var person = personService.findPersonById(registrationRequest.getPersonId());

        var organisationalUnit = organisationalUnitService.findOrganisationalUnitById(
            registrationRequest.getOrganisationalUnitId());

        var newUser =
            new User(registrationRequest.getEmail(), registrationRequest.getPassword(), "",
                registrationRequest.getFirstname(), registrationRequest.getLastName(), true,
                false, preferredLanguage, authority, person, organisationalUnit);

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
    public void updateUser(UserUpdateRequestDTO userUpdateRequest, Integer userId) {
        var userToUpdate = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with given ID does not exist."));

        var preferredLanguage =
            languageService.findLanguageById(userUpdateRequest.getPreferredLanguageId());

        var person = personService.findPersonById(userUpdateRequest.getPersonId());

        var organisationalUnit = organisationalUnitService.findOrganisationalUnitById(
            userUpdateRequest.getOrganisationalUnitId());

        userToUpdate.setEmail(userUpdateRequest.getEmail());
        userToUpdate.setFirstname(userUpdateRequest.getFirstname());
        userToUpdate.setLastName(userUpdateRequest.getLastName());
        userToUpdate.setCanTakeRole(userUpdateRequest.getCanTakeRole());
        userToUpdate.setPreferredLanguage(preferredLanguage);
        userToUpdate.setPerson(person);
        userToUpdate.setOrganisationalUnit(organisationalUnit);

        if (!userUpdateRequest.getOldPassword().equals("") &&
            passwordEncoder.matches(userUpdateRequest.getOldPassword(),
                userToUpdate.getPassword())) {
            userToUpdate.setPassword(passwordEncoder.encode(userUpdateRequest.getNewPassword()));
        } else if (!userUpdateRequest.getOldPassword().equals("")) {
            throw new WrongPasswordProvidedException("Wrong old password provided.");
        }

        userRepository.save(userToUpdate);
    }

    private String createAndSaveRefreshTokenForUser(User user) {
        var refreshTokenValue = UUID.randomUUID().toString();
        var hashedRefreshToken = Hashing.sha256()
            .hashString(refreshTokenValue, StandardCharsets.UTF_8)
            .toString();
        refreshTokenRepository.save(new RefreshToken(hashedRefreshToken, user));

        return refreshTokenValue;
    }

    @Scheduled(cron = "0 */5 * ? * *") // every five minutes
    public void cleanupLongLivedRefreshTokens() {
        var refreshTokens = refreshTokenRepository.findAll();

        var now = new Date();
        var twentyMinutesAgo = new Date(now.getTime() - (20 * 60 * 1000));

        refreshTokens.stream()
            .filter(token -> token.getCreateDate().before(twentyMinutesAgo))
            .forEach(refreshTokenRepository::delete);
    }

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    public void cleanupLongLivedAccountActivationTokens() {
        var activationTokens = userAccountActivationRepository.findAll();

        var now = new Date();
        var sevenDaysAgo = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));

        activationTokens.stream()
            .filter(token -> token.getCreateDate().before(sevenDaysAgo))
            .forEach(userAccountActivationRepository::delete);
    }
}
