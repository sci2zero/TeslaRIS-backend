package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.MonographPublication;

@Repository
public interface MonographPublicationRepository
    extends JpaRepository<MonographPublication, Integer> {

    @Query(value = "SELECT * FROM monograph_publications m WHERE " +
        "(:allTime = TRUE OR m.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "m.approve_status = 1 ORDER BY m.id", nativeQuery = true)
    Page<MonographPublication> findAllModified(Pageable pageable, boolean allTime);

    @Modifying
    @Query("UPDATE MonographPublication mp SET mp.documentDate = :date " +
        "WHERE mp.monograph.id = :monographId AND mp.monographPublicationType = 0")
    void setDateToAggregatedPublications(Integer monographId, String date);

    @Query(value = "SELECT *, 0 AS clazz_ FROM monograph_publications WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<MonographPublication> findMonographPublicationByOldIdsContains(Integer oldId);
}
