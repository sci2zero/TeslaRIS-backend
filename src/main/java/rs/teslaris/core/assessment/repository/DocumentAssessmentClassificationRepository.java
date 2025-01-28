package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;

@Repository
public interface DocumentAssessmentClassificationRepository
    extends JpaRepository<DocumentAssessmentClassification, Integer> {

    @Query("select eac from DocumentAssessmentClassification eac where " +
        "eac.document.id = :documentId order by eac.timestamp desc")
    List<DocumentAssessmentClassification> findAssessmentClassificationsForDocument(
        Integer documentId);

    @Query("select dac from DocumentAssessmentClassification dac where " +
        "dac.document.id = :documentId AND " +
        "dac.commission.id = :commissionId")
    Optional<DocumentAssessmentClassification> findClassificationForDocumentAndCategoryAndCommission(
        Integer documentId, Integer commissionId);
}
