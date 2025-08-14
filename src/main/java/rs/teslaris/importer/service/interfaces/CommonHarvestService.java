package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface CommonHarvestService {

    boolean canPersonScanDataSources(Integer userId);

    boolean canOUEmployeeScanDataSources(Integer organisationUnitId);


}
