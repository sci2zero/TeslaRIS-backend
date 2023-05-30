package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Repository
public interface OrganisationUnitRepository extends JpaRepository<OrganisationUnit, Integer> {
}
