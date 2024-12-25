package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface PublicationSeriesIndicatorRepository extends
    JpaRepository<PublicationSeriesIndicator, Integer> {

    @Query("SELECT psi FROM PublicationSeriesIndicator psi " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "psi.indicator.accessLevel <= :accessLevel")
    List<PublicationSeriesIndicator> findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
        Integer publicationSeriesId,
        AccessLevel accessLevel);

    @Query("SELECT COUNT(ps) > 0 " +
        "FROM PublicationSeriesIndicator ps " +
        "WHERE ps.publicationSeries.id = :publicationSeriesId " +
        "AND ps.indicator.code = :indicatorCode " +
        "AND ps.source = :source " +
        "AND EXTRACT(YEAR FROM ps.fromDate) = :year " +
        "AND (ps.categoryIdentifier = :category OR (:category IS NULL AND ps.categoryIdentifier IS NULL))")
    boolean existsByPublicationSeriesIdAndSourceAndYearAndCategory(Integer publicationSeriesId,
                                                                   EntityIndicatorSource source,
                                                                   Integer year, String category,
                                                                   String indicatorCode);

}
