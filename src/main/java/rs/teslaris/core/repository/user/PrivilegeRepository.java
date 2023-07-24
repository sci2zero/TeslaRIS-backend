package rs.teslaris.core.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.Privilege;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Integer> {
}
