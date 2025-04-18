package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.DocumentIndicator;
import rs.teslaris.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class DocumentIndicatorJPAServiceImpl extends JPAServiceImpl<DocumentIndicator> {


    private final DocumentIndicatorRepository documentIndicatorRepository;

    @Autowired
    public DocumentIndicatorJPAServiceImpl(
        DocumentIndicatorRepository documentIndicatorRepository) {
        this.documentIndicatorRepository = documentIndicatorRepository;
    }

    @Override
    protected JpaRepository<DocumentIndicator, Integer> getEntityRepository() {
        return documentIndicatorRepository;
    }
}
