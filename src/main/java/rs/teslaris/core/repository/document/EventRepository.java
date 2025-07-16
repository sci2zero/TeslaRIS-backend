package rs.teslaris.core.repository.document;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    @Query("SELECT COUNT(p) > 0 FROM Proceedings p JOIN p.event e WHERE e.id = :eventId")
    boolean hasProceedings(Integer eventId);

    @Modifying
    @Query("UPDATE ProceedingsPublication pp SET pp.deleted = true " +
        "WHERE pp.proceedings.event.id = :eventId")
    void deleteAllPublicationsInEvent(Integer eventId);

    @Modifying
    @Query("UPDATE Proceedings p SET p.deleted = true WHERE p.event.id = :eventId")
    void deleteAllProceedingsInEvent(Integer eventId);

    @Query(value = "SELECT * FROM conferences WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Event> findEventByOldIdsContains(Integer oldId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Event e WHERE e.confId = :confId AND (:id IS NULL OR e.id <> :id)")
    boolean existsByConfId(String confId, Integer id);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Event e WHERE e.openAlexId = :openAlexId AND (:id IS NULL OR e.id <> :id)")
    boolean existsByOpenAlexId(String openAlexId, Integer id);

    @Query("SELECT DISTINCT inst.id " +
        "FROM ProceedingsPublication pp " +
        "JOIN pp.proceedings p " +
        "JOIN p.event e " +
        "JOIN pp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE e.id = :eventId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByEventIdAndAuthorContribution(Integer eventId);
}
