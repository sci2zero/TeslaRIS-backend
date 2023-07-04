package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.RefreshToken;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface RefreshTokenRepository extends JPASoftDeleteRepository<RefreshToken> {

    @Query("select t from RefreshToken t where t.refreshTokenValue = :refreshTokenValue")
    Optional<RefreshToken> getRefreshToken(String refreshTokenValue);
}
