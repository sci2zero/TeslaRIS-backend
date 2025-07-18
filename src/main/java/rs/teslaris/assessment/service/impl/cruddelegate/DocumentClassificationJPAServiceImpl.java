package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.repository.classification.DocumentAssessmentClassificationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class DocumentClassificationJPAServiceImpl
    extends JPAServiceImpl<DocumentAssessmentClassification> {

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    @Autowired
    public DocumentClassificationJPAServiceImpl(
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository) {
        this.documentAssessmentClassificationRepository =
            documentAssessmentClassificationRepository;
    }

    @Override
    protected JpaRepository<DocumentAssessmentClassification, Integer> getEntityRepository() {
        return documentAssessmentClassificationRepository;
    }
}
