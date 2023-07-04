package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface AuthorityRepository extends JPASoftDeleteRepository<Authority> {

    Optional<Authority> findByName(String name);
}
