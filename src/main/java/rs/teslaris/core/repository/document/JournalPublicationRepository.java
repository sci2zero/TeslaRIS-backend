package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.JournalPublication;

@Repository
public interface JournalPublicationRepository extends JpaRepository<JournalPublication, Integer> {

    @Query(value = "SELECT * FROM journal_publications jp WHERE " +
        "jp.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' AND " +
        "jp.approve_status = 1", nativeQuery = true)
    Page<JournalPublication> findAllModifiedInLast24Hours(Pageable pageable);
}
