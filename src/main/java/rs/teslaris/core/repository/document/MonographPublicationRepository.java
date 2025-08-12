package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.MonographPublication;

@Repository
public interface MonographPublicationRepository
    extends JpaRepository<MonographPublication, Integer> {

    @Query(value = "SELECT * FROM monograph_publications m WHERE " +
        "m.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' AND " +
        "m.approve_status = 1", nativeQuery = true)
    Page<MonographPublication> findAllModifiedInLast24Hours(Pageable pageable);
}
