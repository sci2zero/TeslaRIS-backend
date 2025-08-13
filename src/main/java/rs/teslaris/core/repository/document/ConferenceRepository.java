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
        "(:allTime = TRUE OR c.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY')", nativeQuery = true)
    Page<Conference> findAllModifiedInLast24Hours(Pageable pageable, boolean allTime);

    @Query("SELECT c FROM Conference c WHERE c.confId = :confId")
    Optional<Conference> findConferenceByConfId(String confId);

    @Query(value = "SELECT *, 0 AS clazz_ FROM conferences WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Conference> findConferenceByOldIdsContains(Integer oldId);

    @Query(value = "SELECT * FROM conferences c WHERE c.id = :conferenceId", nativeQuery = true)
    Optional<Conference> findRaw(Integer conferenceId);
}
