package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Software;

@Repository
public interface SoftwareRepository extends JpaRepository<Software, Integer> {

    @Query(value = "SELECT * FROM software s WHERE " +
        "s.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' AND " +
        "s.approveStatus = 1", nativeQuery = true)
    Page<Software> findAllModifiedInLast24Hours(Pageable pageable);
}
