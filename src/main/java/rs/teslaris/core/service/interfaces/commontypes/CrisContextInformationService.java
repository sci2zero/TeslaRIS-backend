package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CrisContextInformationService extends JPAService<CrisContextInformation> {

    CrisContextInformationDTO readConfigurationForSystem();

    CrisContextInformationDTO saveConfiguration(CrisContextInformationDTO dto);
}
