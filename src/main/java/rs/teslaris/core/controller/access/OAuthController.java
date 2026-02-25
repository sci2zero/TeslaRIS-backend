package rs.teslaris.core.controller.access;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuthController {

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/finish-workflow")
    public ResponseEntity<AuthenticationResponseDTO> finishAuthentication(@RequestParam String code,
                                                                          @RequestParam
                                                                          String identifier) {
        var fingerprint = tokenUtil.generateJWTSecurityFingerprint();

        var authenticationResponse =
            userService.finishOAuthWorkflow(code, identifier, fingerprint);

        var headers = UserController.getJwtSecurityCookieHeader(fingerprint);
        return new ResponseEntity<>(authenticationResponse, headers, HttpStatus.OK);
    }
}
