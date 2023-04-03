package rs.teslaris.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.Authority;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Integer> {
}
