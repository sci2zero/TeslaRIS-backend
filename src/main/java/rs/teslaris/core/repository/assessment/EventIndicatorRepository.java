package rs.teslaris.core.repository.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.assessment.EventIndicator;

@Repository
public interface EventIndicatorRepository extends JpaRepository<EventIndicator, Integer> {
}
