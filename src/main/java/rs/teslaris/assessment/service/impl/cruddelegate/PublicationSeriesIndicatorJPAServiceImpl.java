package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
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
