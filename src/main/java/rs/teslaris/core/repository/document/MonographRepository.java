package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Monograph;

@Repository
public interface MonographRepository extends JpaRepository<Monograph, Integer> {

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Monograph m WHERE m.eISBN = :eISBN AND m.id <> :id")
    boolean existsByeISBN(String eISBN, Integer id);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Monograph m WHERE m.printISBN = :printISBN AND m.id <> :id")
    boolean existsByPrintISBN(String printISBN, Integer id);
}
