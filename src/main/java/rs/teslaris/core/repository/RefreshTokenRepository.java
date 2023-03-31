package rs.teslaris.core.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.core.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    @Query("select t from RefreshToken t where t.refreshTokenValue = :refreshTokenValue")
    Optional<RefreshToken> getRefreshToken(String refreshTokenValue);
}
