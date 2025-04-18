package rs.teslaris.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.PersonAssessmentClassification;

@Repository
public interface PersonAssessmentClassificationRepository extends
    JpaRepository<PersonAssessmentClassification, Integer> {

    @Query("select eac from PersonAssessmentClassification eac where " +
        "eac.person.id = :personId order by eac.timestamp desc")
    List<PersonAssessmentClassification> findAssessmentClassificationsForPerson(Integer personId);
}
