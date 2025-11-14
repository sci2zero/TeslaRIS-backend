package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.configuration.OAuth2Provider;
import rs.teslaris.core.dto.user.ActivateAccountRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.CommissionRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ConfirmEmailUpdateRequestDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForceEmailChangeDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.RefreshTokenRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Traceable
public class UserController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    public static HttpHeaders getJwtSecurityCookieHeader(String fingerprint) {
        var headers = new HttpHeaders();
        headers.add("Set-Cookie",
            "jwt-security-fingerprint=" + fingerprint + "; SameSite=Strict; HttpOnly; Path=/api");

        return headers;
    }

    @GetMapping
    public UserResponseDTO getUser(@RequestHeader("Authorization") String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        return userService.getUserProfile(userId);
    }

    @GetMapping("/search")
    public Page<UserAccountIndex> searchUserAccounts(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(value = "allowedRole", required = false) List<UserRole> allowedRoles,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return userService.searchUserAccounts(tokens, allowedRoles, pageable);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticateUser(
        @RequestBody @Valid AuthenticationRequestDTO authenticationRequest) {
        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse =
            userService.authenticateUser(authenticationManager, authenticationRequest, fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    @Idempotent
    public ResponseEntity<AuthenticationResponseDTO> refreshToken(
        @RequestBody @Valid RefreshTokenRequestDTO refreshTokenRequest) {
        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse =
            userService.refreshToken(refreshTokenRequest.getRefreshTokenValue(), fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @PatchMapping("/activation-status/{id}")
    @PreAuthorize("hasAuthority('DEACTIVATE_USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateOrActivateUserAccount(@PathVariable("id") Integer userId) {
        userService.deactivateUser(userId);
    }

    @PatchMapping("/activate-account")
    public void activateUserAccount(@RequestBody @Valid ActivateAccountRequestDTO activateRequest) {
        userService.activateUserAccount(activateRequest.getActivationToken());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public void initiatePasswordResetProcess(
        @RequestBody @Valid ForgotPasswordRequestDTO forgotPasswordRequest) {
        userService.initiatePasswordResetProcess(forgotPasswordRequest);
    }

    @PatchMapping("/reset-password")
    @Idempotent
    public void resetPassword(@RequestBody @Valid ResetPasswordRequestDTO resetPasswordRequest) {
        userService.resetAccountPassword(resetPasswordRequest);
    }

    @PostMapping("/register-researcher")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public UserResponseDTO registerResearcher(
        @RequestBody @Valid ResearcherRegistrationRequestDTO registrationRequest) {
        var newUser = userService.registerResearcher(registrationRequest);

        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredUILanguage().getLanguageTag(),
            newUser.getPreferredReferenceCataloguingLanguage().getLanguageTag(), null, null,
            newUser.getPerson().getId(), null, newUser.getUserNotificationPeriod());
    }

    @PostMapping("/register-researcher-oauth")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public UserResponseDTO registerResearcherOauth(
        @RequestBody @Valid ResearcherRegistrationRequestDTO registrationRequest,
        @RequestParam OAuth2Provider provider, @RequestParam String identifier) {
        var newUser =
            userService.registerResearcherOAuth(registrationRequest, provider, identifier);

        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredUILanguage().getLanguageTag(),
            newUser.getPreferredReferenceCataloguingLanguage().getLanguageTag(), null, null,
            newUser.getPerson().getId(), null, newUser.getUserNotificationPeriod());
    }

    @GetMapping("/register-researcher-creation-allowed")
    public boolean isNewResearcherCreationAllowed() {
        return userService.isNewResearcherCreationAllowed();
    }

    @PostMapping("/register-employee/{role}")
    @PreAuthorize("hasAuthority('REGISTER_EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public UserResponseDTO registerEmployee(
        @PathVariable String role,
        @RequestBody @Valid EmployeeRegistrationRequestDTO registrationRequest)
        throws NoSuchAlgorithmException {

        UserRole userRole = switch (role) {
            case "institution-admin" -> UserRole.INSTITUTIONAL_EDITOR;
            case "vice-dean-for-science" -> UserRole.VICE_DEAN_FOR_SCIENCE;
            case "institution-librarian" -> UserRole.INSTITUTIONAL_LIBRARIAN;
            case "head-of-library" -> UserRole.HEAD_OF_LIBRARY;
            case "promotion-registry-administrator" -> UserRole.PROMOTION_REGISTRY_ADMINISTRATOR;
            default -> throw new IllegalArgumentException("Invalid employee role: " + role);
        };

        var newUser = userService.registerInstitutionEmployee(registrationRequest, userRole);
        return constructEmployeeResponse(newUser, registrationRequest);
    }

    private UserResponseDTO constructEmployeeResponse(User newUser,
                                                      EmployeeRegistrationRequestDTO registrationRequest) {
        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredUILanguage().getLanguageTag(),
            newUser.getPreferredReferenceCataloguingLanguage().getLanguageTag(),
            registrationRequest.getOrganisationUnitId(), null, null, null,
            newUser.getUserNotificationPeriod());
    }

    @PostMapping("/register-commission")
    @PreAuthorize("hasAuthority('REGISTER_EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public UserResponseDTO registerCommissionUser(
        @RequestBody @Valid CommissionRegistrationRequestDTO registrationRequest)
        throws NoSuchAlgorithmException {
        var newUser = userService.registerCommissionUser(registrationRequest);

        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredUILanguage().getLanguageTag(),
            newUser.getPreferredReferenceCataloguingLanguage().getLanguageTag(),
            registrationRequest.getOrganisationUnitId(), registrationRequest.getCommissionId(),
            null, null,
            newUser.getUserNotificationPeriod());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UPDATE_PROFILE')")
    public ResponseEntity<AuthenticationResponseDTO> updateUser(
        @RequestHeader("Authorization") String bearerToken,
        @RequestBody @Valid UserUpdateRequestDTO updateRequest) {
        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse = userService.updateUser(updateRequest,
            tokenUtil.extractUserIdFromToken(bearerToken), fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @PostMapping("/take-role")
    @PreAuthorize("hasAuthority('TAKE_ROLE')")
    public ResponseEntity<AuthenticationResponseDTO> takeRoleOfUser(
        @RequestBody @Valid TakeRoleOfUserRequestDTO takeRoleRequest) {

        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse = userService.takeRoleOfUser(takeRoleRequest, fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @GetMapping("/take-role")
    @PreAuthorize("hasAuthority('TAKE_ROLE')")
    public List<Integer> getAccountsWithRoleTakingAllowed() {
        return userService.getAccountsWithRoleTakingAllowed();
    }

    @PatchMapping("/allow-role-taking")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ALLOW_ACCOUNT_TAKEOVER')")
    public void allowTakingRoleOfAccount(@RequestHeader("Authorization") String bearerToken) {
        userService.allowTakingRoleOfAccount(bearerToken);
    }

    @GetMapping("/person/{personId}")
    public UserResponseDTO getUserFromPerson(@PathVariable("personId") Integer personId) {
        return userService.getUserFromPerson(personId);
    }

    @PatchMapping("/reset-user-password/{employeeId}")
    @PreAuthorize("hasAuthority('GENERATE_NEW_EMPLOYEE_PASSWORD')")
    public boolean resetPasswordForUser(@PathVariable Integer employeeId) {
        return userService.generateNewPasswordForUser(employeeId);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('DELETE_USER_ACCOUNT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserAccount(@PathVariable Integer userId) {
        userService.deleteUserAccount(userId);
    }

    @DeleteMapping("/migrate/{oldUserId}/{newUserId}")
    @PreAuthorize("hasAuthority('DELETE_USER_ACCOUNT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void migrateUserAccountData(@PathVariable Integer oldUserId,
                                       @PathVariable Integer newUserId) {
        userService.migrateUserAccountData(newUserId, oldUserId);
    }

    @PatchMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutUser(@RequestHeader("Authorization") String bearerToken) {
        userService.logout(tokenUtil.extractJtiFromToken(bearerToken));
    }

    @PatchMapping("/confirm-email-change")
    public boolean confirmEmailChange(@RequestBody ConfirmEmailUpdateRequestDTO request) {
        return userService.confirmEmailChange(request.getConfirmationToken());
    }

    @PatchMapping("/force-email-change/{userId}")
    @PreAuthorize("hasAuthority('FORCE_EMAIL_CHANGE')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forceEmailChange(@PathVariable Integer userId,
                                 @RequestBody ForceEmailChangeDTO request) {
        userService.changeUserEmail(userId, request.newEmail());
    }

    @PatchMapping("/resend-activation-email/{userId}")
    @PreAuthorize("hasAuthority('FORCE_EMAIL_CHANGE')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendActivationEmail(@PathVariable Integer userId) {
        userService.resendUserActivationEmail(userId);
    }
}
