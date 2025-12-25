package rs.teslaris.assessment.repository.classification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;

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
    List<EntityAssessmentClassification> findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
        Integer publicationSeriesId, Integer commissionId, Integer year);

    @Query("SELECT psac FROM PublicationSeriesAssessmentClassification psac WHERE " +
        "psac.publicationSeries.id = :publicationSeriesId AND " +
        "psac.categoryIdentifier = :category AND " +
        "psac.classificationYear = :classificationYear AND " +
        "psac.commission.id = :commissionId")
    Optional<PublicationSeriesAssessmentClassification> findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
        Integer publicationSeriesId, String category, Integer classificationYear,
        Integer commissionId);

    @Modifying
    @Transactional
    @Query("UPDATE PublicationSeriesAssessmentClassification psac " +
        "SET psac.deleted = TRUE WHERE " +
        "psac.publicationSeries.id IN :publicationSeriesIds AND " +
        "psac.classificationYear IN :classificationYears AND " +
        "psac.commission.id = :commissionId")
    void deleteByPublicationSeriesAndYearsAndCommission(List<Integer> publicationSeriesIds,
                                                        List<Integer> classificationYears,
                                                        Integer commissionId);

    @Transactional
    @Modifying
    @Query(value = """
            DELETE FROM entity_assessment_classifications_classification_reason
            WHERE entity_assessment_classification_id IN (
                SELECT id FROM entity_assessment_classifications
                WHERE publication_series_id = :publicationSeriesId
                  AND category_identifier = :category
                  AND classification_year = :classificationYear
                  AND commission_id = :commissionId
            )
        """, nativeQuery = true)
    void deleteClassificationReasonsForPublicationSeriesAndCategoryAndYearAndCommission(
        Integer publicationSeriesId,
        String category,
        Integer classificationYear,
        Integer commissionId);


    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM entity_assessment_classifications 
        WHERE publication_series_id = :publicationSeriesId
          AND category_identifier = :category
          AND classification_year = :classificationYear
          AND commission_id = :commissionId
        """, nativeQuery = true)
    void deleteClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
        Integer publicationSeriesId,
        String category,
        Integer classificationYear,
        Integer commissionId);

    @Query("SELECT psac FROM PublicationSeriesAssessmentClassification psac " +
        "WHERE psac.publicationSeries.id = :publicationSeriesId")
    Page<PublicationSeriesAssessmentClassification> findClassificationsForPublicationSeries(
        Integer publicationSeriesId, Pageable pageable);

    @Query(value = "SELECT eac.id FROM entity_assessment_classifications eac " +
        "WHERE eac.deleted = TRUE " +
        "ORDER BY eac.id",
        countQuery = "SELECT COUNT(*) FROM entity_assessment_classifications WHERE deleted = TRUE",
        nativeQuery = true)
    Page<Integer> findIdsOfDeletedAssessments(Pageable pageable);

    @Query(
        value = "SELECT reason.classification_reason_id " +
            "FROM entity_assessment_classifications_classification_reason reason " +
            "WHERE reason.entity_assessment_classification_id IN :ids",
        nativeQuery = true)
    List<Integer> findReasonIdsForDeletion(List<Integer> ids);


    @Modifying
    @Transactional
    @Query(value = "DELETE FROM entity_assessment_classifications_classification_reason reason " +
        "WHERE reason.entity_assessment_classification_id IN :ids", nativeQuery = true)
    int deleteClassificationReasonsForDeleted(List<Integer> ids);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM multi_lingual_content WHERE id IN :ids", nativeQuery = true)
    void deleteReasonsMarkedAsDeleted(List<Integer> ids);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM entity_assessment_classifications eac " +
        "WHERE eac.id IN :ids", nativeQuery = true)
    int hardDeleteMarkedAsDeleted(List<Integer> ids);
}
