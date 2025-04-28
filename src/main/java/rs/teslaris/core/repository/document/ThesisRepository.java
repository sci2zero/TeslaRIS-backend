package rs.teslaris.core.repository.document;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;

@Repository
public interface ThesisRepository extends JpaRepository<Thesis, Integer> {

    @Query(value = "SELECT * FROM theses t WHERE " +
        "t.isArchived = TRUE AND " +
        "t.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Thesis> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT t FROM Thesis t WHERE t.isOnPublicReview = true")
    List<Thesis> findAllOnPublicReview();

    @Query("SELECT COUNT(t) FROM Thesis t WHERE " +
        "t.thesisDefenceDate >= :startDate AND " +
        "t.thesisDefenceDate <= :endDate AND " +
        "t.thesisType = :type AND " +
        "t.organisationUnit.id IN :institutionIds")
    Integer countDefendedThesesInPeriod(LocalDate startDate, LocalDate endDate, ThesisType type,
                                        List<Integer> institutionIds);

    @Query("SELECT COUNT(t) FROM Thesis t WHERE " +
        "t.topicAcceptanceDate >= :startDate AND " +
        "t.topicAcceptanceDate <= :endDate AND " +
        "t.thesisType = :type AND " +
        "t.organisationUnit.id IN :institutionIds")
    Integer countAcceptedThesesInPeriod(LocalDate startDate, LocalDate endDate, ThesisType type,
                                        List<Integer> institutionIds);

    @Query("SELECT COUNT(DISTINCT t) FROM Thesis t JOIN t.publicReviewStartDates d " +
        "WHERE d >= :startDate AND " +
        "d <= :endDate AND " +
        "t.thesisType = :type AND " +
        "t.organisationUnit.id IN :institutionIds")
    Integer countThesesWithPublicReviewInPeriod(LocalDate startDate, LocalDate endDate,
                                                ThesisType type, List<Integer> institutionIds);

    @Query("SELECT COUNT(DISTINCT t) FROM Thesis t JOIN t.fileItems fi " +
        "WHERE fi.license = 3 AND " +
        "t.thesisDefenceDate >= :startDate AND " +
        "t.thesisDefenceDate <= :endDate AND " +
        "t.thesisType = :type AND " +
        "t.organisationUnit.id IN :institutionIds")
    Integer countPubliclyAvailableDefendedThesesThesesInPeriod(LocalDate startDate,
                                                               LocalDate endDate, ThesisType type,
                                                               List<Integer> institutionIds);

    @Query("SELECT DISTINCT t FROM Thesis t " +
        "LEFT JOIN FETCH t.contributors " +
        "LEFT JOIN FETCH t.fileItems " +
        "LEFT JOIN FETCH t.proofs " +
        "LEFT JOIN FETCH t.preliminaryFiles " +
        "LEFT JOIN FETCH t.preliminarySupplements " +
        "LEFT JOIN FETCH t.commissionReports " +
        "LEFT JOIN t.publicReviewStartDates d " +
        "WHERE t.organisationUnit.id = :institutionId " +
        "AND t.thesisType in :types " +
        "AND (" +
        "(:defended IS NULL OR " +
        "(:defended = TRUE AND t.thesisDefenceDate BETWEEN :startDate AND :endDate) OR " +
        "(:defended = FALSE AND (t.thesisDefenceDate < :startDate OR t.thesisDefenceDate > :endDate OR t.thesisDefenceDate IS NULL))" +
        ") " +
        "OR " +
        "(:putOnReview IS NULL OR " +
        "(:putOnReview = TRUE AND d BETWEEN :startDate AND :endDate) OR " +
        "(:putOnReview = FALSE AND (d < :startDate OR d > :endDate OR d IS NULL))" +
        ")" +
        ")")
    Page<Thesis> findThesesForBackup(LocalDate startDate,
                                     LocalDate endDate,
                                     List<ThesisType> types,
                                     Integer institutionId,
                                     Boolean defended,
                                     Boolean putOnReview,
                                     Pageable pageable);

}
