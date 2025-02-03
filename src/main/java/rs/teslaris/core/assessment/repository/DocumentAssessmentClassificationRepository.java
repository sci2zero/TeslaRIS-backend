package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;

@Repository
public interface DocumentAssessmentClassificationRepository
    extends JpaRepository<DocumentAssessmentClassification, Integer> {

    @Query("select eac from DocumentAssessmentClassification eac where " +
        "eac.document.id = :documentId order by eac.timestamp desc")
    List<DocumentAssessmentClassification> findAssessmentClassificationsForDocument(
        Integer documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentAssessmentClassification dac WHERE dac.document.id = :documentId")
    void deleteByDocumentId(Integer documentId);

}
