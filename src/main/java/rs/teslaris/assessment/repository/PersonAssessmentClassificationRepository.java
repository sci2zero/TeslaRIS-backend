package rs.teslaris.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.PersonAssessmentClassification;

@Repository
public interface PersonAssessmentClassificationRepository extends
    JpaRepository<PersonAssessmentClassification, Integer> {

    @Query("SELECT eac FROM PersonAssessmentClassification eac WHERE " +
        "eac.person.id = :personId ORDER BY eac.timestamp DESC")
    List<PersonAssessmentClassification> findAssessmentClassificationsForPerson(Integer personId);
}
