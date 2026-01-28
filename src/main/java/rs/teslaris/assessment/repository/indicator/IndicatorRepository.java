package rs.teslaris.assessment.repository.indicator;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.Indicator;

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

    @Query(
        value = """
            SELECT i
            FROM Indicator i
            JOIN i.title title
            WHERE title.language.languageTag = :languageTag
            """,
        countQuery = """
            SELECT COUNT(i)
            FROM Indicator i
            WHERE EXISTS (
                SELECT 1
                FROM Indicator i2
                JOIN i2.title title
                WHERE i2 = i
                  AND title.language.languageTag = :languageTag
            )
            """
    )
    Page<Indicator> readAll(String languageTag, Pageable pageable);

}
