package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.AuthenticationResponseDTO;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil tokenUtil;


    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticateUser(
        @RequestBody AuthenticationRequestDTO authenticationRequest) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(),
                authenticationRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();
        var token = tokenUtil.generateToken(authentication, fingerprint);

        var headers = new HttpHeaders();
        headers.add("Set-Cookie",
            "jwt-security-fingerprint=" + fingerprint + "; SameSite=Strict; HttpOnly; Path=/api");
        return new ResponseEntity<>(new AuthenticationResponseDTO(token), headers, HttpStatus.OK);
    }
}
