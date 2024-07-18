package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.EventsRelation;

@Repository
public interface EventsRelationRepository extends JpaRepository<EventsRelation, Integer> {

    @Query("select er from EventsRelation er where er.source.id = :eventId")
    List<EventsRelation> getRelationsForOneTimeEvent(Integer eventId);

    @Query("select er from EventsRelation er where er.target.id = :eventId")
    List<EventsRelation> getRelationsForEvent(Integer eventId);

    @Query("select count(er) > 0 from EventsRelation er " +
        "where (er.source.id = :sourceId and er.target.id = :targetId) or" +
        "(er.target.id = :sourceId and er.source.id = :targetId and er.eventsRelationType = 2)")
    boolean relationExists(Integer sourceId, Integer targetId);
}
