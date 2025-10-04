package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.OAuthCode;

@Repository
public interface OAuthCodeRepository extends JpaRepository<OAuthCode, Integer> {

    @Query("SELECT oc FROM OAuthCode oc WHERE " +
        "oc.code = :code AND " +
        "oc.identifier = :identifier")
    Optional<OAuthCode> getCodeForCodeAndIdentifier(String code, String identifier);

    @Modifying
    @Query("DELETE FROM OAuthCode oc WHERE oc.identifier = :identifier")
    void deleteByIdentifier(String identifier);
}
