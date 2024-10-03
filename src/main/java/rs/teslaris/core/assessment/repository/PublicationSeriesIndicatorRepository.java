package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;

@Repository
public interface PublicationSeriesIndicatorRepository extends
    JpaRepository<PublicationSeriesIndicator, Integer> {

    @Query("select psi from PublicationSeriesIndicator psi " +
        "where psi.publicationSeries.id = :publicationSeriesId and " +
        "psi.indicator.accessLevel <= :accessLevel")
    List<PublicationSeriesIndicator> findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
        Integer publicationSeriesId,
        IndicatorAccessLevel accessLevel);
}
