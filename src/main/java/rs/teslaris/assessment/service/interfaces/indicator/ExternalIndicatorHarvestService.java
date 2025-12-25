package rs.teslaris.assessment.service.interfaces.indicator;

import org.springframework.stereotype.Service;

@Service
public interface ExternalIndicatorHarvestService {

    void harvestAllManually();

    void performPersonIndicatorHarvest();

    void performOUIndicatorDeduction();

    void performIndicatorHavestForSinglePerson(Integer personId);

    void performIndicatorDeductionForSingleInstitution(Integer organisationUnitId);
}
