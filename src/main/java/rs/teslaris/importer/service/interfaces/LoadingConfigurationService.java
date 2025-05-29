package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.model.configuration.LoadingConfiguration;

@Service
public interface LoadingConfigurationService extends JPAService<LoadingConfiguration> {

    void saveLoadingConfiguration(Integer institutionId,
                                  LoadingConfigurationDTO loadingConfiguration);

    LoadingConfigurationDTO getLoadingConfigurationForUser(Integer userId);
}
