package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.UserAccountActivation;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface UserAccountActivationRepository
    extends JPASoftDeleteRepository<UserAccountActivation> {

    @Deprecated
    Optional<UserAccountActivation> findByActivationToken(String activationToken);


    Optional<UserAccountActivation> findByActivationTokenAndDeletedIsFalse(String activationToken);
}
