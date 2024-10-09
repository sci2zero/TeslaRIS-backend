package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.IndicatorDTO;
import rs.teslaris.core.assessment.dto.IndicatorResponseDTO;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface IndicatorService extends JPAService<Indicator> {

    Page<IndicatorResponseDTO> readAllIndicators(Pageable pageable);

    IndicatorResponseDTO readIndicatorById(Integer indicatorId);

    Indicator getIndicatorByCode(String code);

    Indicator createIndicator(IndicatorDTO indicatorDTO);

    void updateIndicator(Integer indicatorId, IndicatorDTO indicatorDTO);

    void deleteIndicator(Integer indicatorId);
}
