package rs.teslaris.assessment.service.interfaces.indicator;

import org.springframework.stereotype.Service;

@Service
public interface ExternalIndicatorHarvestService {

    void performPersonIndicatorHarvest();

    void performOUIndicatorDeduction();
}
