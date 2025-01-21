package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.ApplicableEntityType;
import rs.teslaris.core.assessment.model.AssessmentClassification;

@Repository
public interface AssessmentClassificationRepository
    extends JpaRepository<AssessmentClassification, Integer> {

    @Query("select count(eac) > 0 from EntityAssessmentClassification eac where eac.assessmentClassification.id = :assessmentClassificationId")
    boolean isInUse(Integer assessmentClassificationId);

    @Query("SELECT ac FROM AssessmentClassification ac JOIN ac.applicableTypes at WHERE at IN :applicableEntityTypes")
    List<AssessmentClassification> getAssessmentClassificationsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    Optional<AssessmentClassification> findByCode(String code);
}
