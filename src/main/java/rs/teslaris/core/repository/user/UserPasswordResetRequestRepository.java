package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.UserPasswordResetRequest;

@Repository
public interface UserPasswordResetRequestRepository
    extends JpaRepository<UserPasswordResetRequest, Integer> {

    Optional<UserPasswordResetRequest> findByPasswordResetToken(String passwordResetToken);
}
