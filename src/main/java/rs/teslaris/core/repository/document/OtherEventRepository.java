package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.OtherEvent;

@Repository
public interface OtherEventRepository extends JpaRepository<OtherEvent, Integer> {

    @Query(value = "SELECT * FROM other_events oe WHERE " +
        "(:allTime = TRUE OR oe.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        " ORDER BY oe.id", nativeQuery = true)
    Page<OtherEvent> findAllModified(Pageable pageable, boolean allTime);
}
