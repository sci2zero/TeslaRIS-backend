package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.repository.DocumentAssessmentClassificationRepository;
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
