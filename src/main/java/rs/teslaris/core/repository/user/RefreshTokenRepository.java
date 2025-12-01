package rs.teslaris.core.repository.user;

import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteAllByUserId(Integer userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt " +
        "WHERE EXISTS (" +
        "  SELECT u FROM User u JOIN u.authority a " +
        "  WHERE u = rt.user AND a.name <> 'ADMIN'" +
        ")")
    void deleteAllNonAdminRefreshTokens();
}
