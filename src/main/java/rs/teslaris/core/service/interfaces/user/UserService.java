package rs.teslaris.core.service.interfaces.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.RegistrationRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.model.user.User;

@Service
public interface UserService extends UserDetailsService {

    User loadUserById(Integer userId);

    int getUserOrganisationUnitId(Integer userId);

    Integer getPersonIdForUser(Integer userId);

    boolean isUserAResearcher(Integer userId, Integer personId);

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
