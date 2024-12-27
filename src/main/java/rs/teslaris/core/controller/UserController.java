package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
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
import rs.teslaris.core.dto.user.ActivateAccountRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.RefreshTokenRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping
    public UserResponseDTO getUser(@RequestHeader("Authorization") String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        return userService.getUserProfile(userId);
    }

    @GetMapping("/search")
    public Page<UserAccountIndex> searchUserAccounts(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return userService.searchUserAccounts(tokens, pageable);
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
            newUser.getPreferredLanguage().getLanguageCode(), null, newUser.getPerson().getId(),
            null,
            newUser.getUserNotificationPeriod());
    }

    @PostMapping("/register-institution-admin")
    @PreAuthorize("hasAuthority('REGISTER_EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public UserResponseDTO registerInstitutionAdmin(
        @RequestBody @Valid EmployeeRegistrationRequestDTO registrationRequest) {
        var newUser = userService.registerInstitutionAdmin(registrationRequest);

        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredLanguage().getLanguageCode(),
            registrationRequest.getOrganisationUnitId(), null, null,
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

    private HttpHeaders getJwtSecurityCookieHeader(String fingerprint) {
        var headers = new HttpHeaders();
        headers.add("Set-Cookie",
            "jwt-security-fingerprint=" + fingerprint + "; SameSite=Strict; HttpOnly; Path=/api");

        return headers;
    }
}
