package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Conference;

@Repository
public interface ConferenceRepository extends JpaRepository<Conference, Integer> {

    @Query(value = "SELECT * FROM conferences c WHERE " +
        "c.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Conference> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("select c from Conference c where c.confId = :confId")
    Optional<Conference> findConferenceByConfId(String confId);
}
