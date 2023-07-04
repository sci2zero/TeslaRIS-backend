package rs.teslaris.core.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PrivilegeRepository extends JPASoftDeleteRepository<Privilege> {
}
