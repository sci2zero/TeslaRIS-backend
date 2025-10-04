package rs.teslaris.core.repository.user;

import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    @Query("SELECT t FROM RefreshToken t WHERE t.refreshTokenValue = :refreshTokenValue")
    Optional<RefreshToken> getRefreshToken(String refreshTokenValue);

    @Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId")
    Optional<RefreshToken> findByUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.createDate < :date")
    void deleteAllByCreateDateBefore(Date date);
}
