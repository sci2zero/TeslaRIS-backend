package rs.teslaris.core.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.identifier.PublicationSeriesIdentifier;
import rs.teslaris.core.repository.identifier.PublicationSeriesIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PublicationSeriesIdentifierJPAServiceImpl
    extends JPAServiceImpl<PublicationSeriesIdentifier> {

    private final PublicationSeriesIdentifierRepository publicationSeriesIdentifierRepository;


    @Autowired
    public PublicationSeriesIdentifierJPAServiceImpl(
        PublicationSeriesIdentifierRepository publicationSeriesIdentifierRepository) {
        this.publicationSeriesIdentifierRepository = publicationSeriesIdentifierRepository;
    }

    @Override
    protected JpaRepository<PublicationSeriesIdentifier, Integer> getEntityRepository() {
        return publicationSeriesIdentifierRepository;
    }
}
