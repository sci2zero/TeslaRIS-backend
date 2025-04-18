package rs.teslaris.assessment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.EntityIndicatorSource;
import rs.teslaris.assessment.model.PublicationSeriesIndicator;
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
}
