package rs.teslaris.core.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.Indicator;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Integer> {

    @Query("select count(ei) > 0 from EntityIndicator ei where ei.indicator.id = :indicatorId")
    boolean isInUse(Integer indicatorId);

    @Query("select count(i) > 0 from Indicator i where i.code = :code and i.id != :indicatorId")
    boolean indicatorCodeInUse(String code, Integer indicatorId);

    Indicator findByCode(String code);

    @Query(value =
        "SELECT i FROM Indicator i LEFT JOIN i.title title LEFT JOIN i.description description " +
            "WHERE title.language.languageTag = :languageTag AND description.language.languageTag = :languageTag",
        countQuery =
            "SELECT count(DISTINCT i) FROM Indicator i LEFT JOIN i.title title LEFT JOIN i.description description " +
                "WHERE title.language.languageTag = :languageTag AND description.language.languageTag = :languageTag")
    Page<Indicator> readAll(String languageTag, Pageable pageable);
}
