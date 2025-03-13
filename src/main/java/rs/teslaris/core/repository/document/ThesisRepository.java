package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Thesis;

@Repository
public interface ThesisRepository extends JpaRepository<Thesis, Integer> {

    @Query(value = "SELECT * FROM theses t WHERE " +
        "t.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Thesis> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT t FROM Thesis t WHERE t.isOnPublicReview = true")
    List<Thesis> findAllOnPublicReview();
}
