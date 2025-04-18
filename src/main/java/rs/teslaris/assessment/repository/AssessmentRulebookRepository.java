package rs.teslaris.assessment.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentRulebook;

@Repository
public interface AssessmentRulebookRepository extends
    JpaRepository<AssessmentRulebook, Integer> {

    @Query("SELECT am FROM AssessmentMeasure am WHERE am.rulebook.id = :rulebookId")
    Page<AssessmentMeasure> readAssessmentMeasuresForRulebook(Pageable pageable,
                                                              Integer rulebookId);

    @Modifying
    @Query("UPDATE AssessmentRulebook ar SET ar.isDefault = false WHERE ar.id != :rulebookId")
    void setAllOthersAsNonDefault(Integer rulebookId);

    @Query("SELECT ar FROM AssessmentRulebook ar WHERE ar.isDefault = true")
    Optional<AssessmentRulebook> findDefaultRulebook();

    @Query(value =
        "SELECT ar FROM AssessmentRulebook ar LEFT JOIN ar.name name LEFT JOIN ar.description description " +
            "WHERE name.language.languageTag = :languageTag AND description.language.languageTag = :languageTag",
        countQuery =
            "SELECT count(DISTINCT ar) FROM AssessmentRulebook ar LEFT JOIN ar.name name LEFT JOIN ar.description description " +
                "WHERE name.language.languageTag = :languageTag AND description.language.languageTag = :languageTag")
    Page<AssessmentRulebook> readAll(String languageTag, Pageable pageable);
}
