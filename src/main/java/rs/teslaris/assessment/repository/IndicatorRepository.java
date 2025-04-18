package rs.teslaris.assessment.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.ApplicableEntityType;
import rs.teslaris.assessment.model.Indicator;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Integer> {

    @Query("SELECT count(ei) > 0 FROM EntityIndicator ei WHERE ei.indicator.id = :indicatorId")
    boolean isInUse(Integer indicatorId);

    @Query("SELECT count(i) > 0 FROM Indicator i WHERE i.code = :code AND i.id != :indicatorId")
    boolean indicatorCodeInUse(String code, Integer indicatorId);

    @Query("SELECT i FROM Indicator i JOIN i.applicableTypes at WHERE at IN :applicableEntityTypes")
    List<Indicator> getIndicatorsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    Indicator findByCode(String code);

    @Query(value =
        "SELECT i FROM Indicator i LEFT JOIN i.title title LEFT JOIN i.description description " +
            "WHERE title.language.languageTag = :languageTag AND description.language.languageTag = :languageTag",
        countQuery =
            "SELECT count(DISTINCT i) FROM Indicator i LEFT JOIN i.title title LEFT JOIN i.description description " +
                "WHERE title.language.languageTag = :languageTag AND description.language.languageTag = :languageTag")
    Page<Indicator> readAll(String languageTag, Pageable pageable);
}
