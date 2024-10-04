package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.EventAssessmentClassification;

@Repository
public interface EventAssessmentClassificationRepository extends
    JpaRepository<EventAssessmentClassification, Integer> {

    @Query("select eac from EventAssessmentClassification eac where " +
        "eac.event.id = :eventId order by eac.timestamp desc")
    List<EventAssessmentClassification> findAssessmentClassificationsForEvent(Integer eventId);
}
