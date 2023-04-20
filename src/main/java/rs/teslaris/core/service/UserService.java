package rs.teslaris.core.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.AuthenticationResponseDTO;
import rs.teslaris.core.dto.RegistrationRequestDTO;
import rs.teslaris.core.dto.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.UserUpdateRequestDTO;
import rs.teslaris.core.model.user.User;

@Service
public interface UserService extends UserDetailsService {

    User loadUserById(Integer userId);

    AuthenticationResponseDTO authenticateUser(AuthenticationManager authernticationManager,
                                               AuthenticationRequestDTO authenticationRequest,
                                               String fingerprint);

    AuthenticationResponseDTO refreshToken(String refreshTokenValue, String fingerprint);

    AuthenticationResponseDTO takeRoleOfUser(TakeRoleOfUserRequestDTO takeRoleOfUserRequest,
                                             String fingerprint);

    void allowTakingRoleOfAccount(String bearerToken);

    void deactivateUser(Integer userId);

    void activateUserAccount(String activationTokenValue);

    User registerUser(RegistrationRequestDTO registrationRequest);

    AuthenticationResponseDTO updateUser(UserUpdateRequestDTO userUpdateRequest, Integer userID,
                                         String fingerprint);
}
