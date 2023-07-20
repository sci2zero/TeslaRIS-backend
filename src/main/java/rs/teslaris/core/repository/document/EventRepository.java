package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    @Query("select count(p) > 0 from Proceedings p join p.event e where e.id = :eventId")
    boolean hasProceedings(Integer eventId);
}
