package rs.teslaris.assessment.repository.indicator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface PublicationSeriesIndicatorRepository extends
    JpaRepository<PublicationSeriesIndicator, Integer> {

    @Query("SELECT psi FROM PublicationSeriesIndicator psi " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId")
    Page<PublicationSeriesIndicator> findIndicatorsForPublicationSeries(Integer publicationSeriesId,
                                                                        Pageable pageable);

    @Query("SELECT psi FROM PublicationSeriesIndicator psi " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "extract(year from psi.fromDate) >= :fromYear AND " +
        "extract(year from psi.fromDate) <= :toYear AND " +
        "psi.indicator.code = :code")
    List<PublicationSeriesIndicator> findIndicatorsForPublicationSeriesAndCodeInPeriod(
        Integer publicationSeriesId, String code, Integer fromYear, Integer toYear);

    @Query("SELECT psi FROM PublicationSeriesIndicator psi " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "psi.indicator.accessLevel <= :accessLevel")
    List<PublicationSeriesIndicator> findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
        Integer publicationSeriesId,
        AccessLevel accessLevel);

    @Query("SELECT psi FROM PublicationSeriesIndicator psi JOIN FETCH psi.indicator " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "extract(year from psi.fromDate) = :year AND " +
        "psi.indicator.code = :code AND " +
        "psi.source = :source")
    List<PublicationSeriesIndicator> findIndicatorsForPublicationSeriesAndCodeAndSourceAndYear(
        Integer publicationSeriesId, String code, Integer year, EntityIndicatorSource source);

    @Query("SELECT psi FROM PublicationSeriesIndicator psi JOIN FETCH psi.indicator " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "(extract(year from psi.fromDate) = :year OR " +
        "(extract(year from psi.fromDate) < :year AND psi.toDate IS NULL)) AND " +
        "psi.source = :source")
    List<PublicationSeriesIndicator> findCombinedIndicatorsForPublicationSeriesAndIndicatorSourceAndYear(
        Integer publicationSeriesId, Integer year, EntityIndicatorSource source);

    @Query("SELECT DISTINCT psi.publicationSeries.id FROM PublicationSeriesIndicator psi " +
        "WHERE psi.categoryIdentifier = :category AND " +
        "extract(year from psi.fromDate) = :year AND " +
        "psi.source = :source")
    List<Integer> findIndicatorsForCategoryAndYearAndSource(
        String category, Integer year, EntityIndicatorSource source);

    @Query("SELECT DISTINCT psi FROM PublicationSeriesIndicator psi JOIN FETCH psi.indicator " +
        "WHERE psi.publicationSeries.id IN :journalIds AND " +
        "psi.indicator.code = :code AND " +
        "extract(year from psi.fromDate) = :year AND " +
        "psi.source = :source")
    List<PublicationSeriesIndicator> findJournalIndicatorsForIdsAndCodeAndYearAndSource(
        List<Integer> journalIds, String code, Integer year, EntityIndicatorSource source);

    @Query("SELECT ps " +
        "FROM PublicationSeriesIndicator ps " +
        "WHERE ps.publicationSeries.id = :publicationSeriesId " +
        "AND ps.indicator.code = :indicatorCode " +
        "AND ps.source = :source " +
        "AND ps.fromDate = :date " +
        "AND (ps.categoryIdentifier = :category OR (:category IS NULL AND ps.categoryIdentifier IS NULL))")
    Optional<PublicationSeriesIndicator> existsByPublicationSeriesIdAndSourceAndYearAndCategory(
        Integer publicationSeriesId,
        EntityIndicatorSource source,
        LocalDate date, String category,
        String indicatorCode);

    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM entity_indicators psi
        USING indicators i
        WHERE psi.indicator_id = i.id
          AND psi.publication_series_id = :publicationSeriesId
          AND i.code = :indicatorCode
          AND psi.source = :source
          AND psi.from_date = :date
          AND (psi.category_identifier = :category OR (:category IS NULL AND psi.category_identifier IS NULL))
          AND psi.entity_type = 'PUBLICATION_SERIES_INDICATOR'
        """, nativeQuery = true)
    void deleteByPublicationSeriesIdAndSourceAndYearAndCategory(
        Integer publicationSeriesId,
        EntityIndicatorSource source,
        LocalDate date,
        String category,
        String indicatorCode);

    @Transactional
    @Query("SELECT psi FROM PublicationSeriesIndicator psi JOIN FETCH psi.indicator " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND " +
        "psi.indicator.code = :code")
    Optional<PublicationSeriesIndicator> findIndicatorForCodeAndPublicationSeriesId(String code,
                                                                                    Integer publicationSeriesId);
}
