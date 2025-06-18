package rs.teslaris.assessment.service.interfaces.indicator;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.indicator.IndicatorDTO;
import rs.teslaris.assessment.dto.indicator.IndicatorResponseDTO;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface IndicatorService extends JPAService<Indicator> {

    Page<IndicatorResponseDTO> readAllIndicators(Pageable pageable, String language);

    List<IndicatorResponseDTO> getIndicatorsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    IndicatorResponseDTO readIndicatorById(Integer indicatorId);

    AccessLevel readIndicatorAccessLEvel(Integer indicatorId);

    Indicator getIndicatorByCode(String code);

    Indicator createIndicator(IndicatorDTO indicatorDTO);

    void updateIndicator(Integer indicatorId, IndicatorDTO indicatorDTO);

    void deleteIndicator(Integer indicatorId);
}
