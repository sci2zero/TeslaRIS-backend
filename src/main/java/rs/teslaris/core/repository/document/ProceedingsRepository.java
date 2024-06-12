package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Proceedings;

@Repository
public interface ProceedingsRepository extends JpaRepository<Proceedings, Integer> {

    @Query("select p from Proceedings p join p.event e where e.id = :eventId")
    List<Proceedings> findProceedingsForEventId(Integer eventId);
}
