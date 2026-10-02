package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.PublicConfigurationDTO;

@Service
public interface PublicConfigurationService {

    PublicConfigurationDTO readPublicConfiguration();
}
