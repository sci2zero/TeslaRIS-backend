package rs.teslaris.core.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.model.AssessmentRulebook;

@Repository
public interface AssessmentRulebookRepository extends
    JpaRepository<AssessmentRulebook, Integer> {

    @Query("select am from AssessmentMeasure am where am.rulebook.id = :rulebookId")
    Page<AssessmentMeasure> readAssessmentMeasuresForRulebook(Pageable pageable,
                                                              Integer rulebookId);

    @Query(value =
        "SELECT ar FROM AssessmentRulebook ar LEFT JOIN ar.name name LEFT JOIN ar.description description " +
            "WHERE name.language.languageTag = :languageTag AND description.language.languageTag = :languageTag",
        countQuery =
            "SELECT count(DISTINCT ar) FROM AssessmentRulebook ar LEFT JOIN ar.name name LEFT JOIN ar.description description " +
                "WHERE name.language.languageTag = :languageTag AND description.language.languageTag = :languageTag")
    Page<AssessmentRulebook> readAll(String languageTag, Pageable pageable);
}
