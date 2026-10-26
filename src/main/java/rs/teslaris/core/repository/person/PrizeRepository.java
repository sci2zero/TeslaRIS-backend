package rs.teslaris.core.repository.person;

import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Prize;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {

    @Query("SELECT COUNT(p) > 0 FROM Prize p JOIN p.title t " +
        "WHERE p.person.id = :personId " +
        "AND p.date = :date " +
        "AND LOWER(TRIM(t.content)) IN :titles")
    boolean existsForPersonWithTitleAndDate(Integer personId, LocalDate date,
                                            Collection<String> titles);

    @Query("SELECT COUNT(p) > 0 FROM Prize p JOIN p.title t " +
        "WHERE p.person.id = :personId " +
        "AND p.date IS NULL " +
        "AND LOWER(TRIM(t.content)) IN :titles")
    boolean existsForPersonWithTitleAndNoDate(Integer personId, Collection<String> titles);
}
