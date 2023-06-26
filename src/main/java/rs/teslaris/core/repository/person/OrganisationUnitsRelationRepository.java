package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Repository
public interface OrganisationUnitsRelationRepository
    extends JpaRepository<OrganisationUnitsRelation, Integer> {

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.sourceOrganisationUnit.id = :sourceId " +
        " and our.targetOrganisationUnit.id = :targetId " + "and our.approveStatus = 1")
    Page<OrganisationUnitsRelation> getRelationsForOrganisationUnits(Pageable pageable,
                                                                     Integer sourceId,
                                                                     Integer targetId);

    List<OrganisationUnitsRelation> findBySourceOrganisationUnit(Integer sourceOrganisationId);

    List<OrganisationUnitsRelation> findBySourceOrganisationUnitAndRelationType(
        Integer sourceOrganisationId, OrganisationUnitRelationType relationType);

    List<OrganisationUnitsRelation> findByTargetOrganisationUnit(Integer destinationOrganisationId);
}
