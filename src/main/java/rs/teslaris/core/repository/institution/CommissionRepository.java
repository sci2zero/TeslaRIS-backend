package rs.teslaris.core.repository.institution;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.Commission;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    @Query("SELECT COUNT(eac) > 0 FROM EntityAssessmentClassification eac " +
        "WHERE eac.commission.id = :commissionId")
    boolean isInUse(Integer commissionId);

    @Query(value =
        "SELECT c FROM Commission c LEFT JOIN c.description description WHERE description.language.languageTag = :languageTag AND " +
            "LOWER(description.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))",
        countQuery =
            "SELECT count(c) FROM Commission c JOIN c.description d WHERE d.language.languageTag = :languageTag AND " +
                "LOWER(d.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))")
    Page<Commission> searchCommissions(String searchExpression, String languageTag,
                                       Pageable pageable);

    @Query(value =
        "SELECT c FROM Commission c LEFT JOIN c.description description " +
            "WHERE description.language.languageTag = :languageTag",
        countQuery =
            "SELECT count(DISTINCT c) FROM Commission c LEFT JOIN c.description description " +
                "WHERE description.language.languageTag = :languageTag")
    Page<Commission> readAll(String languageTag, Pageable pageable);

    @Query("SELECT c FROM Commission c LEFT JOIN FETCH c.relations LEFT JOIN FETCH c.relations.targetCommissions WHERE c.id = :commissionId")
    Optional<Commission> findOneWithRelations(Integer commissionId);

    @Query("SELECT c.id FROM Commission c LEFT JOIN EventAssessmentClassification eac " +
        "ON eac.commission.id = c.id " +
        "WHERE eac.event.id = :eventId")
    List<Integer> findCommissionsThatClassifiedEvent(Integer eventId);

    @Query(
        "SELECT c.id FROM Commission c LEFT JOIN PublicationSeriesAssessmentClassification psac " +
            "ON psac.commission.id = c.id " +
            "WHERE psac.publicationSeries.id = :journalId")
    List<Integer> findCommissionsThatClassifiedJournal(Integer journalId);

    @Query("SELECT c.id FROM Commission c LEFT JOIN DocumentAssessmentClassification eac " +
        "ON eac.commission.id = c.id " +
        "WHERE eac.document.id = :documentId")
    List<Integer> findCommissionsThatAssessedDocument(Integer documentId);

    @Query("""
            SELECT NEW rs.teslaris.core.repository.institution.AssessmentClassificationBasicInfo(
                eac.commission.id, eac.assessmentClassification.code, eac.manual
            )
            FROM DocumentAssessmentClassification eac
            WHERE eac.document.id = :documentId
              AND eac.commission.id IN :commissionIds
        """)
    List<AssessmentClassificationBasicInfo> findAssessmentClassificationBasicInfoForDocumentAndCommissions(
        Integer documentId, List<Integer> commissionIds);

    @Modifying
    @Query("UPDATE Commission c SET c.isDefault = false WHERE c.id != :commissionId")
    void setOthersAsNonDefault(Integer commissionId);

    Optional<Commission> findCommissionByIsDefaultTrue();
}
