package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    @Query("select t from RefreshToken t where t.refreshTokenValue = :refreshTokenValue")
    Optional<RefreshToken> getRefreshToken(String refreshTokenValue);

    @Query("select t from RefreshToken t where t.user.id = :userId")
    Optional<RefreshToken> findByUserId(Integer userId);
}
