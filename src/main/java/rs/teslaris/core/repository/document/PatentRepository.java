package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Patent;

@Repository
public interface PatentRepository extends JpaRepository<Patent, Integer> {

    @Query(value = "SELECT * FROM patents p WHERE " +
        "(:allTime = TRUE OR p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "p.approve_status = 1 ORDER BY p.id", nativeQuery = true)
    Page<Patent> findAllModified(Pageable pageable, boolean allTime);

    @Query(value = "SELECT *, 0 AS clazz_ FROM patents WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Patent> findPatentByOldIdsContains(Integer oldId);
}
