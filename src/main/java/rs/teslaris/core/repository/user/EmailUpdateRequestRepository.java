package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.EmailUpdateRequest;

@Repository
public interface EmailUpdateRequestRepository extends JpaRepository<EmailUpdateRequest, Integer> {

    Optional<EmailUpdateRequest> findByEmailUpdateToken(String emailUpdateToken);
}
