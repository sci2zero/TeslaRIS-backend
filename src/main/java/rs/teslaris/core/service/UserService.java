package rs.teslaris.core.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.TakeRoleOfUserRequestDTO;

@Service
public interface UserService extends UserDetailsService {

    String authenticateUser(AuthenticationManager authernticationManager,
                            AuthenticationRequestDTO authenticationRequest, String fingerprint);

    String takeRoleOfUser(TakeRoleOfUserRequestDTO takeRoleOfUserRequest, String fingerprint);

    void allowTakingRoleOfAccount(String bearerToken);
}
