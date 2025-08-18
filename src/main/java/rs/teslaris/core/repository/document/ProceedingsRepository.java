package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Proceedings;

@Repository
public interface ProceedingsRepository extends JpaRepository<Proceedings, Integer> {

    @Query("SELECT p FROM Proceedings p JOIN p.event e " +
        "WHERE e.id = :eventId AND p.approveStatus = 1")
    List<Proceedings> findProceedingsForEventId(Integer eventId);

    @Query("SELECT count(pp) > 0 FROM ProceedingsPublication pp " +
        "WHERE pp.proceedings.id = :proceedingsId")
    boolean hasPublications(Integer proceedingsId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Proceedings p WHERE (p.eISBN = :eISBN OR p.printISBN = :eISBN) AND (:id IS NULL OR p.id <> :id)")
    boolean existsByeISBN(String eISBN, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Proceedings p WHERE (p.printISBN = :printISBN OR p.eISBN = :printISBN) AND (:id IS NULL OR p.id <> :id)")
    boolean existsByPrintISBN(String printISBN, Integer id);

    @Query(value = "SELECT * FROM proceedings p WHERE " +
        "(:allTime = TRUE OR p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "p.approve_status = 1", nativeQuery = true)
    Page<Proceedings> findAllModified(Pageable pageable, boolean allTime);

    @Modifying
    @Query("UPDATE ProceedingsPublication pp SET pp.deleted = true " +
        "WHERE pp.proceedings.id = :proceedingsId")
    void deleteAllPublicationsInProceedings(Integer proceedingsId);

    @Query("SELECT p FROM Proceedings p WHERE " +
        "p.printISBN = :printISBN OR " +
        "p.printISBN = :eISBN OR " +
        "p.eISBN = :eISBN OR " +
        "p.eISBN = :printISBN")
    List<Proceedings> findByISBN(String eISBN, String printISBN);

    @Query(value = "SELECT * FROM proceedings p WHERE p.id = :proceedingsId", nativeQuery = true)
    Optional<Proceedings> findRaw(Integer proceedingsId);
}
