package rs.teslaris.core.repository.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Repository
public interface OrganisationUnitsRelationRepository
    extends JpaRepository<OrganisationUnitsRelation, Integer> {
}
