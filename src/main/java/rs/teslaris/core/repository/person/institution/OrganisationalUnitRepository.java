package rs.teslaris.core.repository.person.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Repository
public interface OrganisationalUnitRepository extends JpaRepository<OrganisationUnit, Integer> {
}
