package rs.teslaris.core.repository.user;

import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.UserAccountActivation;

@Repository
public interface UserAccountActivationRepository
    extends JpaRepository<UserAccountActivation, Integer> {

    Optional<UserAccountActivation> findByActivationToken(String activationToken);

    @Modifying
    @Query("DELETE FROM UserAccountActivation uaa WHERE uaa.createDate < :date")
    void deleteAllByCreateDateBefore(Date date);
}
