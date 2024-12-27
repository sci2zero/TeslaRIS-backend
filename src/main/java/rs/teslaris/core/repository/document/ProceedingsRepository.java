package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Proceedings;

@Repository
public interface ProceedingsRepository extends JpaRepository<Proceedings, Integer> {

    @Query("select p from Proceedings p join p.event e where e.id = :eventId and p.approveStatus = 1")
    List<Proceedings> findProceedingsForEventId(Integer eventId);

    @Query("select count(pp) > 0 from ProceedingsPublication pp where pp.proceedings.id = :proceedingsId")
    boolean hasPublications(Integer proceedingsId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Proceedings p WHERE (p.eISBN = :eISBN OR p.printISBN = :eISBN) AND p.id <> :id")
    boolean existsByeISBN(String eISBN, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Proceedings p WHERE (p.printISBN = :printISBN OR p.eISBN = :printISBN) AND p.id <> :id")
    boolean existsByPrintISBN(String printISBN, Integer id);

    @Query(value = "SELECT * FROM proceedings p WHERE " +
        "p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Proceedings> findAllModifiedInLast24Hours(Pageable pageable);

    @Modifying
    @Query("update ProceedingsPublication pp set pp.deleted = true where pp.proceedings.id = :proceedingsId")
    void deleteAllPublicationsInProceedings(Integer proceedingsId);
}
