package rs.teslaris.core.repository.user;

import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByPasswordResetToken(String passwordResetToken);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.createDate < :date")
    void deleteAllByCreateDateBefore(Date date);
}
