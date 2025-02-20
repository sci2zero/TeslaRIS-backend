package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("select eac from EventAssessmentClassification eac where eac.event.id = :eventId")
    Page<EventAssessmentClassification> findAssessmentClassificationsForEvent(Integer eventId,
                                                                              Pageable pageable);

    @Query(
        "SELECT eac FROM EventAssessmentClassification eac JOIN FETCH eac.assessmentClassification WHERE " +
            "eac.event.id = :eventId AND " +
            "eac.commission.id = :commissionId AND " +
            "eac.classificationYear = :year")
    Optional<EventAssessmentClassification> findAssessmentClassificationsForEventAndCommissionAndYear(
        Integer eventId, Integer commissionId, Integer year);

    @Query(
        "SELECT eac FROM EventAssessmentClassification eac WHERE " +
            "eac.event.id = :eventId AND " +
            "eac.commission.id = :commissionId")
    Optional<EventAssessmentClassification> findAssessmentClassificationsForEventAndCommission(
        Integer eventId, Integer commissionId);
}
