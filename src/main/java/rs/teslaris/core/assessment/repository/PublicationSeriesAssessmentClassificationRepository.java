package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;

@Repository
public interface PublicationSeriesAssessmentClassificationRepository extends
    JpaRepository<PublicationSeriesAssessmentClassification, Integer> {

    @Query("SELECT psac FROM PublicationSeriesAssessmentClassification psac WHERE " +
        "psac.publicationSeries.id = :publicationSeriesId ORDER BY psac.timestamp DESC")
    List<PublicationSeriesAssessmentClassification> findAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId);

    @Query(
        "SELECT psac FROM PublicationSeriesAssessmentClassification psac JOIN FETCH psac.assessmentClassification WHERE " +
            "psac.publicationSeries.id = :publicationSeriesId AND " +
            "psac.commission.id = :commissionId AND " +
            "psac.classificationYear = :year")
    Optional<EntityAssessmentClassification> findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
        Integer publicationSeriesId, Integer commissionId, Integer year);

    @Query("SELECT psac FROM PublicationSeriesAssessmentClassification psac WHERE " +
        "psac.publicationSeries.id = :publicationSeriesId AND " +
        "psac.categoryIdentifier = :category AND " +
        "psac.classificationYear = :classificationYear AND " +
        "psac.commission.id = :commissionId")
    Optional<PublicationSeriesAssessmentClassification> findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
        Integer publicationSeriesId, String category, Integer classificationYear,
        Integer commissionId);

    @Query("SELECT psac FROM PublicationSeriesAssessmentClassification psac WHERE psac.publicationSeries.id = :publicationSeriesId")
    Page<PublicationSeriesAssessmentClassification> findClassificationsForPublicationSeries(
        Integer publicationSeriesId, Pageable pageable);
}
