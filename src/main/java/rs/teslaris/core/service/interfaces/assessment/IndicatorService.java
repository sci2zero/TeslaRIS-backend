package rs.teslaris.core.service.interfaces.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.assessment.IndicatorDTO;
import rs.teslaris.core.model.assessment.Indicator;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface IndicatorService extends JPAService<Indicator> {

    Page<IndicatorDTO> readAllIndicators(Pageable pageable);

    IndicatorDTO readIndicatorById(Integer indicatorId);

    Indicator createIndicator(IndicatorDTO indicatorDTO);

    void updateIndicator(Integer indicatorId, IndicatorDTO indicatorDTO);

    void deleteIndicator(Integer indicatorId);
}
