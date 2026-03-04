package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Exhibition;

@Repository
public interface ExhibitionRepository extends JpaRepository<Exhibition, Integer> {

    @Query(value = "SELECT *, 0 AS clazz_ FROM exhibitions WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Exhibition> findExhibitionByOldIdsContains(Integer oldId);

    @Query(value = "SELECT * FROM exhibitions e WHERE e.id = :exhibitionId", nativeQuery = true)
    Optional<Exhibition> findRaw(Integer exhibitionId);

    @Query(value = "SELECT * FROM exhibitions e WHERE " +
        "(:allTime = TRUE OR e.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        " ORDER BY e.id", nativeQuery = true)
    Page<Exhibition> findAllModified(Pageable pageable, boolean allTime);
}
