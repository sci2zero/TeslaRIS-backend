package rs.teslaris.core.repository.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.assessment.Indicator;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Integer> {

    @Query("select count(ei) > 0 from EntityIndicator ei where ei.indicator.id = :indicatorId")
    boolean isInUse(Integer indicatorId);
}
