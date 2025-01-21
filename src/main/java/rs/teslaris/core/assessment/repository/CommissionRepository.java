package rs.teslaris.core.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.Commission;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    @Query("select count(eac) > 0 from EntityAssessmentClassification eac where eac.commission.id = :commissionId")
    boolean isInUse(Integer commissionId);

    @Query(value =
        "SELECT c FROM Commission c LEFT JOIN c.description description WHERE description.language.languageTag = :languageTag AND " +
            "LOWER(description.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))",
        countQuery =
            "SELECT count(c) FROM Commission c JOIN c.description d WHERE d.language.languageTag = :languageTag AND " +
                "LOWER(d.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))")
    Page<Commission> searchCommissions(String searchExpression, String languageTag,
                                       Pageable pageable);
}
