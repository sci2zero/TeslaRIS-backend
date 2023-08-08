package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.UserAccountActivation;

@Repository
public interface UserAccountActivationRepository
    extends JpaRepository<UserAccountActivation, Integer> {

    Optional<UserAccountActivation> findByActivationToken(String activationToken);

}
