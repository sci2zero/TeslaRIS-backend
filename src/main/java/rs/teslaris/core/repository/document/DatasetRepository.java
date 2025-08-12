package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Dataset;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Integer> {

    @Query(value = "SELECT * FROM datasets d WHERE " +
        "d.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' AND " +
        "d.approve_status = 1", nativeQuery = true)
    Page<Dataset> findAllModifiedInLast24Hours(Pageable pageable);
}
