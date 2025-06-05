package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.EventsRelation;

@Repository
public interface EventsRelationRepository extends JpaRepository<EventsRelation, Integer> {

    @Query("SELECT er FROM EventsRelation er WHERE er.source.id = :eventId")
    List<EventsRelation> getRelationsForOneTimeEvent(Integer eventId);

    @Query("SELECT er FROM EventsRelation er WHERE er.target.id = :eventId")
    List<EventsRelation> getRelationsForEvent(Integer eventId);

    @Query("SELECT COUNT(er) > 0 FROM EventsRelation er " +
        "WHERE (er.source.id = :sourceId AND er.target.id = :targetId) OR" +
        "(er.target.id = :sourceId AND er.source.id = :targetId AND er.eventsRelationType = 2)")
    boolean relationExists(Integer sourceId, Integer targetId);
}
