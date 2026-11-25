package rs.teslaris.revisioner.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.teslaris.revisioner.model.qualityassessment.AssessedEntityKey;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;

public interface DataQualityAssessmentRepository
    extends JpaRepository<DataQualityAssessment, Integer> {

    @Query("select a from DataQualityAssessment a join fetch a.revision where a.id = :assessmentId")
    Optional<DataQualityAssessment> findWithRevisionById(
        @Param("assessmentId") Integer assessmentId);

    /**
     * Keyset step over assessed entities. The row comparison is an index condition on
     * idx_revision_entity_lookup, so each call costs the same regardless of how far it is.
     */
    @Query("select distinct new rs.teslaris.revisioner.model.qualityassessment" +
        ".AssessedEntityKey(r.entityType, r.entityId) " +
        "from EntityRevision r " +
        "where (r.entityType, r.entityId) > (:afterEntityType, :afterEntityId) " +
        "and exists (select 1 from DataQualityAssessment a where a.revision = r) " +
        "order by r.entityType, r.entityId")
    List<AssessedEntityKey> findAssessedEntityKeysAfter(
        @Param("afterEntityType") String afterEntityType,
        @Param("afterEntityId") Integer afterEntityId,
        Limit limit);

    /**
     * Loads every assessment of the entities in an inclusive key range, ordered so each
     * (entityType, entityId, profileName) chain is contiguous and oldest first.
     */
    @Query("select a from DataQualityAssessment a join fetch a.revision r " +
        "where (r.entityType, r.entityId) >= (:fromEntityType, :fromEntityId) " +
        "and (r.entityType, r.entityId) <= (:toEntityType, :toEntityId) " +
        "order by r.entityType, r.entityId, a.profileName, a.finishedAt, a.id")
    List<DataQualityAssessment> findAllWithRevisionForEntityRange(
        @Param("fromEntityType") String fromEntityType,
        @Param("fromEntityId") Integer fromEntityId,
        @Param("toEntityType") String toEntityType,
        @Param("toEntityId") Integer toEntityId);
}
