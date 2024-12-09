package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PublicationSeriesIndicatorJPAServiceImpl
    extends JPAServiceImpl<PublicationSeriesIndicator> {

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    @Autowired
    public PublicationSeriesIndicatorJPAServiceImpl(
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository) {
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
    }

    @Override
    protected JpaRepository<PublicationSeriesIndicator, Integer> getEntityRepository() {
        return publicationSeriesIndicatorRepository;
    }
}
