package rs.teslaris.core.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.identifier.DocumentIdentifier;
import rs.teslaris.core.repository.identifier.DocumentIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class DocumentIdentifierJPAServiceImpl extends JPAServiceImpl<DocumentIdentifier> {

    private final DocumentIdentifierRepository documentIdentifierRepository;


    @Autowired
    public DocumentIdentifierJPAServiceImpl(
        DocumentIdentifierRepository documentIdentifierRepository) {
        this.documentIdentifierRepository = documentIdentifierRepository;
    }

    @Override
    protected JpaRepository<DocumentIdentifier, Integer> getEntityRepository() {
        return documentIdentifierRepository;
    }
}
