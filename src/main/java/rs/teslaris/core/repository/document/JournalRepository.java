package rs.teslaris.core.repository.document;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Journal;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Integer> {

    @Query("SELECT COUNT(p) > 0 FROM JournalPublication p " +
        "JOIN p.journal j WHERE j.id = :journalId")
    boolean hasPublication(Integer journalId);

    @Modifying
    @Query("UPDATE JournalPublication jp SET jp.deleted = true WHERE jp.journal.id = :journalId")
    void deleteAllPublicationsInJournal(Integer journalId);

    @Query(value = "SELECT *, 0 AS clazz_ FROM journals WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Journal> findByOldIdsContains(Integer oldId);

    @Query(value = "SELECT * FROM journals j WHERE " +
        "j.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Journal> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT DISTINCT inst.id " +
        "FROM JournalPublication jp " +
        "JOIN jp.journal j " +
        "JOIN jp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE j.id = :journalId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByJournalIdAndAuthorContribution(Integer journalId);

    @Query(value = "SELECT * FROM journals j WHERE j.id = :journalId", nativeQuery = true)
    Optional<Journal> findRaw(Integer journalId);
}
