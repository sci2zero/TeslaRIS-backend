package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Monograph;

@Repository
public interface MonographRepository extends JpaRepository<Monograph, Integer> {

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Monograph m WHERE (m.eISBN = :eISBN OR m.printISBN = :eISBN) AND (:id IS NULL OR m.id <> :id)")
    boolean existsByeISBN(String eISBN, Integer id);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Monograph m WHERE (m.printISBN = :printISBN OR m.eISBN = :printISBN) AND (:id IS NULL OR m.id <> :id)")
    boolean existsByPrintISBN(String printISBN, Integer id);

    @Query(value = "SELECT * FROM monographs m WHERE " +
        "m.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Monograph> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM MonographPublication p " +
        "JOIN p.monograph m " +
        "WHERE m.id = :monographId")
    boolean hasPublication(Integer monographId);

    @Modifying
    @Query("UPDATE MonographPublication mp SET mp.deleted = true " +
        "WHERE mp.monograph.id = :monographId")
    void deleteAllPublicationsInMonograph(Integer monographId);

    @Query(value = "SELECT * FROM monographs m WHERE m.id = :monographId", nativeQuery = true)
    Optional<Monograph> findRaw(Integer monographId);
}
