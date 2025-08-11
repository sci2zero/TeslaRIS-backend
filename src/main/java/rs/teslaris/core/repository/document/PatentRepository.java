package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Patent;

@Repository
public interface PatentRepository extends JpaRepository<Patent, Integer> {

    @Query(value = "SELECT * FROM patents p WHERE " +
        "p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' AND " +
        "p.approve_status = 1", nativeQuery = true)
    Page<Patent> findAllModifiedInLast24Hours(Pageable pageable);
}
