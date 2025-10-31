package rs.teslaris.core.service.interfaces.user;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import rs.teslaris.core.configuration.OAuth2Provider;
import rs.teslaris.core.dto.user.AuthenticationRequestDTO;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.dto.user.CommissionRegistrationRequestDTO;
import rs.teslaris.core.dto.user.EmployeeRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.ResearcherRegistrationRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;
import rs.teslaris.core.dto.user.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.dto.user.UserUpdateRequestDTO;
import rs.teslaris.core.indexmodel.UserAccountIndex;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface UserService extends UserDetailsService, JPAService<User> {

    Page<UserAccountIndex> searchUserAccounts(List<String> tokens, List<UserRole> allowedRoles,
                                              Pageable pageable);

    UserResponseDTO getUserProfile(Integer userId);

    int getUserOrganisationUnitId(Integer userId);

    int getUserCommissionId(Integer userId);

    Integer getPersonIdForUser(Integer userId);

    boolean isUserAResearcher(Integer userId, Integer personId);

    AuthenticationResponseDTO authenticateUser(AuthenticationManager authernticationManager,
                                               AuthenticationRequestDTO authenticationRequest,
                                               String fingerprint);

    AuthenticationResponseDTO finishOAuthWorkflow(String code, String identifier,
                                                  String fingerprint);

    AuthenticationResponseDTO refreshToken(String refreshTokenValue, String fingerprint);

    AuthenticationResponseDTO takeRoleOfUser(TakeRoleOfUserRequestDTO takeRoleOfUserRequest,
                                             String fingerprint);

    void allowTakingRoleOfAccount(String bearerToken);

    void deactivateUser(Integer userId);

    void activateUserAccount(String activationTokenValue);

    User registerResearcher(ResearcherRegistrationRequestDTO registrationRequest);

    User registerResearcherOAuth(ResearcherRegistrationRequestDTO registrationRequest,
                                 OAuth2Provider oAuth2Provider, String identifier);

    User registerInstitutionEmployee(EmployeeRegistrationRequestDTO registrationRequest,
                                     UserRole userRole) throws NoSuchAlgorithmException;

    User registerCommissionUser(CommissionRegistrationRequestDTO registrationRequest)
        throws NoSuchAlgorithmException;

    AuthenticationResponseDTO updateUser(UserUpdateRequestDTO userUpdateRequest, Integer userID,
                                         String fingerprint);

    void initiatePasswordResetProcess(ForgotPasswordRequestDTO forgotPasswordSubmission);

    void resetAccountPassword(ResetPasswordRequestDTO resetPasswordRequest);

    void updateResearcherCurrentOrganisationUnitIfBound(Integer personId);

    UserResponseDTO getUserFromPerson(Integer personId);

    List<Integer> getAccountsWithRoleTakingAllowed();

    CompletableFuture<Void> reindexUsers();

    List<Commission> findCommissionForOrganisationUnitId(Integer organisationUnitId);

    List<User> findAllCommissionUsers();

    List<User> findAllSystemAdminUsers();

    List<User> findAllLibrarianUsers();

    List<User> findInstitutionalEditorUsersForInstitutionId(Integer institutionId);

    void deleteUserAccount(Integer userId);

    void migrateUserAccountData(Integer userToUpdateId, Integer userToDeleteId);

    boolean generateNewPasswordForUser(Integer userId);

    void logout(String jti);

    boolean confirmEmailChange(String emailUpdateToken);

    boolean isNewResearcherCreationAllowed();
}
