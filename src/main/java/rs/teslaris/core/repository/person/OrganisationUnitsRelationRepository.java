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

    @Query("SELECT our FROM OrganisationUnitsRelation our" +
        " WHERE our.sourceOrganisationUnit.id = :sourceId" +
        " AND our.approveStatus = 1")
    List<OrganisationUnitsRelation> getRelationsForOrganisationUnits(Integer sourceId);

    @Query("SELECT our FROM OrganisationUnitsRelation our" +
        " WHERE our.sourceOrganisationUnit.id = :sourceId" +
        " AND our.approveStatus = 1 AND our.relationType = 0")
    Optional<OrganisationUnitsRelation> getSuperOU(Integer sourceId);

    @Query("SELECT our FROM OrganisationUnitsRelation our" +
        " WHERE our.targetOrganisationUnit.id = :sourceId" +
        " AND our.approveStatus = 1 AND our.relationType = 0")
    List<OrganisationUnitsRelation> getOuSubUnits(Integer sourceId);

    @Query("SELECT our FROM OrganisationUnitsRelation our" +
        " WHERE our.sourceOrganisationUnit.id = :sourceId" +
        " AND our.approveStatus = 1 AND our.relationType = 1")
    List<OrganisationUnitsRelation> getSuperOUsMemberOf(Integer sourceId);

    @Query(value = """
        WITH RECURSIVE hierarchy AS (
            SELECT our.source_organisation_unit_id, our.target_organisation_unit_id
            FROM organisation_units_relations our
            WHERE our.source_organisation_unit_id = :sourceId
              AND our.approve_status = 1
              AND our.relation_type = 0
            UNION ALL
            SELECT our.source_organisation_unit_id, our.target_organisation_unit_id
            FROM organisation_units_relations our
            INNER JOIN hierarchy h ON our.target_organisation_unit_id = h.source_organisation_unit_id
            WHERE our.approve_status = 1 AND our.relation_type = 0
        )
        SELECT target_organisation_unit_id FROM hierarchy
        """, nativeQuery = true)
    List<Integer> getSuperOUsRecursive(Integer sourceId);

    @Query(value = """
        WITH RECURSIVE hierarchy AS (
            SELECT our.target_organisation_unit_id, our.source_organisation_unit_id
            FROM organisation_units_relations our
            WHERE our.target_organisation_unit_id = :sourceId
              AND our.approve_status = 1
              AND our.relation_type = 0
            UNION ALL
            SELECT our.target_organisation_unit_id, our.source_organisation_unit_id
            FROM organisation_units_relations our
            INNER JOIN hierarchy h ON our.source_organisation_unit_id = h.target_organisation_unit_id
            WHERE our.approve_status = 1 AND our.relation_type = 0
        )
        SELECT source_organisation_unit_id FROM hierarchy
        """, nativeQuery = true)
    List<Integer> getSubOUsRecursive(Integer sourceId);

    @Query(value = """
        WITH RECURSIVE hierarchy AS (
            SELECT our.source_organisation_unit_id, our.target_organisation_unit_id
            FROM organisation_units_relations our
            WHERE our.source_organisation_unit_id = :sourceId
              AND our.approve_status = 1
              AND our.relation_type = 0
            UNION ALL
            SELECT our.source_organisation_unit_id, our.target_organisation_unit_id
            FROM organisation_units_relations our
            INNER JOIN hierarchy h ON our.target_organisation_unit_id = h.source_organisation_unit_id
            WHERE our.approve_status = 1 AND our.relation_type = 0
        )
        SELECT DISTINCT source_organisation_unit_id
        FROM hierarchy
        WHERE target_organisation_unit_id = :topLevelId
        """, nativeQuery = true)
    Integer getOneLevelBelowTopOU(Integer sourceId, Integer topLevelId);


    List<OrganisationUnitsRelation> findBySourceOrganisationUnitIdAndRelationType(
        Integer sourceOrganisationId, OrganisationUnitRelationType relationType);
}
