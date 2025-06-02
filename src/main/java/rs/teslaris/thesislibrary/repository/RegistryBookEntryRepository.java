package rs.teslaris.thesislibrary.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;

@Repository
public interface RegistryBookEntryRepository extends JpaRepository<RegistryBookEntry, Integer> {

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE " +
        "rbe.promotion.id = :promotionId AND " +
        "rbe.promotion.finished = false")
    Page<RegistryBookEntry> getBookEntriesForPromotion(Integer promotionId, Pageable pageable);

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE rbe.promotion IS NULL")
    Page<RegistryBookEntry> getNonPromotedBookEntries(Pageable pageable);

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE " +
        "rbe.promotion IS NULL AND " +
        "rbe.dissertationInformation.organisationUnit.id IN :allowedInstitutionIds")
    Page<RegistryBookEntry> getNonPromotedBookEntries(List<Integer> allowedInstitutionIds,
                                                      Pageable pageable);

    @Query("SELECT MAX(rbe.registryBookNumber) FROM RegistryBookEntry rbe WHERE " +
        "rbe.registryBookInstitution.id = :institutionId")
    Integer getLastRegistryBookNumber(Integer institutionId);

    @Query("SELECT MAX(rbe.schoolYearOrdinalNumber) FROM RegistryBookEntry rbe WHERE " +
        "rbe.promotionSchoolYear = :schoolYear")
    Integer getLastRegistrySchoolYearNumber(String schoolYear);

    Optional<RegistryBookEntry> findByAttendanceIdentifier(String attendanceIdentifier);

    @Query("SELECT rbe.id FROM RegistryBookEntry rbe WHERE rbe.thesis.id = :thesisId")
    Integer hasThesisRegistryBookEntry(Integer thesisId);

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE " +
        "rbe.promotion.promotionDate >= :from AND " +
        "rbe.promotion.promotionDate <= :to")
    List<RegistryBookEntry> getRegistryBookEntriesForPeriod(LocalDate from, LocalDate to);

    @Query("""
        SELECT rbe FROM RegistryBookEntry rbe
        WHERE rbe.registryBookInstitution.id = :institutionId
          AND rbe.promotion.finished = true
          AND rbe.promotion.promotionDate BETWEEN :from AND :to
          AND (
            (
                LOWER(CONCAT(rbe.personalInformation.authorName.firstname, ' ', rbe.personalInformation.authorName.lastname)) LIKE LOWER(CONCAT('%', :authorNameLat, '%')) AND
                LOWER(rbe.dissertationInformation.acquiredTitle) LIKE LOWER(CONCAT('%', :authorTitleLat, '%'))
            )
            OR
            (
                LOWER(CONCAT(rbe.personalInformation.authorName.firstname, ' ', rbe.personalInformation.authorName.lastname)) LIKE LOWER(CONCAT('%', :authorNameCyr, '%')) AND
                LOWER(rbe.dissertationInformation.acquiredTitle) LIKE LOWER(CONCAT('%', :authorTitleCyr, '%'))
            )
            OR
            (
                LOWER(CONCAT(rbe.personalInformation.authorName.firstname, ' ', rbe.personalInformation.authorName.lastname)) LIKE LOWER(CONCAT('%', :authorNameCyr, '%')) AND
                LOWER(rbe.dissertationInformation.acquiredTitle) LIKE LOWER(CONCAT('%', :authorTitleLat, '%'))
            )
            OR
            (
                LOWER(CONCAT(rbe.personalInformation.authorName.firstname, ' ', rbe.personalInformation.authorName.lastname)) LIKE LOWER(CONCAT('%', :authorNameLat, '%')) AND
                LOWER(rbe.dissertationInformation.acquiredTitle) LIKE LOWER(CONCAT('%', :authorTitleCyr, '%'))
            )
          )
        """)
    Page<RegistryBookEntry> getRegistryBookEntriesForInstitutionAndPeriod(
        Integer institutionId,
        LocalDate from,
        LocalDate to,
        String authorNameLat,
        String authorTitleLat,
        String authorNameCyr,
        String authorTitleCyr,
        Pageable pageable);

    @Query("SELECT COUNT(rbe) FROM RegistryBookEntry rbe WHERE " +
        "rbe.registryBookInstitution.id = :institutionId AND " +
        "rbe.previousTitleInformation.academicTitle < 4 AND " +
        "rbe.promotion.finished = true AND " +
        "rbe.promotion.promotionDate >= :from AND " +
        "rbe.promotion.promotionDate <= :to")
    Integer getRegistryBookCountForInstitutionAndPeriodNewPromotion(Integer institutionId,
                                                                    LocalDate from,
                                                                    LocalDate to);

    @Query("SELECT COUNT(rbe) FROM RegistryBookEntry rbe WHERE " +
        "rbe.registryBookInstitution.id = :institutionId AND " +
        "rbe.previousTitleInformation.academicTitle = 4 AND " +
        "rbe.promotion.finished = true AND " +
        "rbe.promotion.promotionDate >= :from AND " +
        "rbe.promotion.promotionDate <= :to")
    Integer getRegistryBookCountForInstitutionAndPeriodOldPromotion(Integer institutionId,
                                                                    LocalDate from,
                                                                    LocalDate to);

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE " +
        "rbe.registryBookInstitution.id = :institutionId AND " +
        "rbe.promotion.finished = true AND " +
        "rbe.promotion.promotionDate >= :from AND " +
        "rbe.promotion.promotionDate <= :to")
    Page<RegistryBookEntry> getRegistryBookCountForInstitutionAndPeriod(Integer institutionId,
                                                                        LocalDate from,
                                                                        LocalDate to,
                                                                        Pageable pageable);
}
