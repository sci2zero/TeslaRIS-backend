package rs.teslaris.assessment.repository.classification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;

@Repository
public interface AssessmentClassificationRepository
    extends JpaRepository<AssessmentClassification, Integer> {

    @Query(
        value = """
            SELECT ac
            FROM AssessmentClassification ac
            LEFT JOIN ac.title title
                 WITH title.language.languageTag = :languageTag
            """,
        countQuery = """
            SELECT COUNT(ac)
            FROM AssessmentClassification ac
            """
    )
    Page<AssessmentClassification> readAll(String languageTag, Pageable pageable);

    @Query("SELECT COUNT(eac) > 0 FROM EntityAssessmentClassification eac " +
        "WHERE eac.assessmentClassification.id = :assessmentClassificationId")
    boolean isInUse(Integer assessmentClassificationId);

    @Query("SELECT ac FROM AssessmentClassification ac " +
        "JOIN ac.applicableTypes at " +
        "WHERE at IN :applicableEntityTypes")
    List<AssessmentClassification> getAssessmentClassificationsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    Optional<AssessmentClassification> findByCode(String code);
}
