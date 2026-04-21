package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.EventIdentifier;

@Repository
public interface EventIdentifierRepository extends JpaRepository<EventIdentifier, Integer> {

    @Query("SELECT ei FROM EventIdentifier ei " +
        "WHERE ei.event.id = :eventId AND ei.identifier.accessLevel <= :accessLevel")
    List<EventIdentifier> findIdentifiersForEventAndIdentifierAccessLevel(Integer eventId,
                                                                          AccessLevel accessLevel);
}
