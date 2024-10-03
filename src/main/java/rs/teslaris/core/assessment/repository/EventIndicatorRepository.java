package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.EventIndicator;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;

@Repository
public interface EventIndicatorRepository extends JpaRepository<EventIndicator, Integer> {

    @Query("select ei from EventIndicator ei " +
        "where ei.event.id = :eventId and ei.indicator.accessLevel <= :accessLevel")
    List<EventIndicator> findIndicatorsForEventAndIndicatorAccessLevel(Integer eventId,
                                                                       IndicatorAccessLevel accessLevel);
}
