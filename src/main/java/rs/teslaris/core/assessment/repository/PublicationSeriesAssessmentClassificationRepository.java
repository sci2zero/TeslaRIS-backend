package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;

@Repository
public interface PublicationSeriesAssessmentClassificationRepository extends
    JpaRepository<PublicationSeriesAssessmentClassification, Integer> {

    @Query("select psac from PublicationSeriesAssessmentClassification psac where " +
        "psac.publicationSeries.id = :publicationSeriesId order by psac.timestamp desc")
    List<PublicationSeriesAssessmentClassification> findAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId);

    @Query("select psac from PublicationSeriesAssessmentClassification psac where " +
        "psac.publicationSeries.id = :publicationSeriesId AND " +
        "psac.categoryIdentifier = :category AND " +
        "psac.classificationYear = :classificationYear")
    Optional<PublicationSeriesAssessmentClassification> findClassificationForPublicationSeriesAndCategoryAndYear(
        Integer publicationSeriesId, String category, Integer classificationYear);
}
