package rs.teslaris.core.repository.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Repository
public interface OrganisationUnitsRelationRepository
    extends JpaRepository<OrganisationUnitsRelation, Integer> {

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.sourceOrganisationUnit.id = :sourceId" +
        " and our.approveStatus = 1")
    List<OrganisationUnitsRelation> getRelationsForOrganisationUnits(Integer sourceId);

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.sourceOrganisationUnit.id = :sourceId" +
        " and our.approveStatus = 1 and our.relationType = 0")
    Optional<OrganisationUnitsRelation> getSuperOU(Integer sourceId);

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.targetOrganisationUnit.id = :sourceId" +
        " and our.approveStatus = 1 and our.relationType = 0")
    List<OrganisationUnitsRelation> getOuSubUnits(Integer sourceId);

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.sourceOrganisationUnit.id = :sourceId" +
        " and our.approveStatus = 1 and our.relationType = 1")
    List<OrganisationUnitsRelation> getSuperOUsMemberOf(Integer sourceId);

    List<OrganisationUnitsRelation> findBySourceOrganisationUnitIdAndRelationType(
        Integer sourceOrganisationId, OrganisationUnitRelationType relationType);
}
