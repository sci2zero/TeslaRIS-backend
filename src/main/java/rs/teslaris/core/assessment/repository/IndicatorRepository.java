package rs.teslaris.core.assessment.repository;

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
}
