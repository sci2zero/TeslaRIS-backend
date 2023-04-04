package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.ActivateAccountRequestDTO;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.AuthenticationResponseDTO;
import rs.teslaris.core.dto.RefreshTokenRequestDTO;
import rs.teslaris.core.dto.RegistrationRequestDTO;
import rs.teslaris.core.dto.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.UserResponseDTO;
import rs.teslaris.core.service.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticateUser(
        @RequestBody AuthenticationRequestDTO authenticationRequest) {
        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse =
            userService.authenticateUser(authenticationManager, authenticationRequest, fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponseDTO> refreshToken(
        @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
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

    @PostMapping("/activate-account")
    public void activateUserAccount(@RequestBody ActivateAccountRequestDTO activateRequest) {
        userService.activateUserAccount(activateRequest.getActivationToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO registerUser(@RequestBody RegistrationRequestDTO registrationRequest) {
        var newUser = userService.registerUser(registrationRequest);

        return new UserResponseDTO(newUser.getId(), newUser.getEmail(), newUser.getFirstname(),
            newUser.getLastName(), newUser.getLocked(), newUser.getCanTakeRole(),
            newUser.getPreferredLanguage().getValue());
    }

    @PostMapping("/take-role")
    @PreAuthorize("hasAuthority('TAKE_ROLE')")
    public ResponseEntity<AuthenticationResponseDTO> takeRoleOfUser(
        @RequestBody TakeRoleOfUserRequestDTO takeRoleRequest) {

        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse = userService.takeRoleOfUser(takeRoleRequest, fingerprint);

        var headers = getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }

    @PatchMapping("/allow-role-taking")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ALLOW_ACCOUNT_TAKEOVER')")
    public void allowTakingRoleOfAccount(@RequestHeader("Authorization") String bearerToken) {
        userService.allowTakingRoleOfAccount(bearerToken);
    }

    private HttpHeaders getJwtSecurityCookieHeader(String fingerprint) {
        var headers = new HttpHeaders();
        headers.add("Set-Cookie",
            "jwt-security-fingerprint=" + fingerprint + "; SameSite=Strict; HttpOnly; Path=/api");

        return headers;
    }
}
