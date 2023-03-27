package rs.teslaris.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.Privilege;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Integer> {
}
