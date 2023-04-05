package rs.teslaris.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.OrganisationalUnit;

@Repository
public interface OrganisationalUnitRepository extends JpaRepository<OrganisationalUnit, Integer> {
}
