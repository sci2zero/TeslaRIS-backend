package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.FeatureModuleTogglesDTO;
import rs.teslaris.core.model.commontypes.FeatureModuleToggles;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface FeatureModuleTogglesService extends JPAService<FeatureModuleToggles> {

    FeatureModuleTogglesDTO readConfigurationForSystem();

    FeatureModuleTogglesDTO saveConfiguration(FeatureModuleTogglesDTO dto);
}
