package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface OrganisationUnitsRelationRepository
    extends JPASoftDeleteRepository<OrganisationUnitsRelation> {

    @Query("select our from OrganisationUnitsRelation our" +
        " where our.deleted = false and our.sourceOrganisationUnit.id = :sourceId " +
        " and our.targetOrganisationUnit.id = :targetId " + "and our.approveStatus = 1")
    Page<OrganisationUnitsRelation> getRelationsForOrganisationUnits(Pageable pageable,
                                                                     Integer sourceId,
                                                                     Integer targetId);

    @Deprecated(forRemoval = true)
    List<OrganisationUnitsRelation> findBySourceOrganisationUnit(Integer sourceOrganisationId);


    List<OrganisationUnitsRelation> findBySourceOrganisationUnitAndDeletedIsFalse(
        Integer sourceOrganisationId);


    @Deprecated(forRemoval = true)
    List<OrganisationUnitsRelation> findBySourceOrganisationUnitAndRelationType(
        Integer sourceOrganisationId, OrganisationUnitRelationType relationType);


    List<OrganisationUnitsRelation> findBySourceOrganisationUnitAndRelationTypeAndDeletedIsFalse(
        Integer sourceOrganisationId, OrganisationUnitRelationType relationType);

    @Deprecated(forRemoval = true)
    List<OrganisationUnitsRelation> findByTargetOrganisationUnit(Integer destinationOrganisationId);

    List<OrganisationUnitsRelation> findByTargetOrganisationUnitAndDeletedIsFalse(
        Integer destinationOrganisationId);
}
