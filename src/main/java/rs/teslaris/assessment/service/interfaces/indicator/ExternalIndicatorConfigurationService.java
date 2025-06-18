package rs.teslaris.assessment.service.interfaces.indicator;

import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.indicator.ExternalIndicatorConfigurationDTO;
import rs.teslaris.assessment.model.indicator.ExternalIndicatorConfiguration;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ExternalIndicatorConfigurationService
    extends JPAService<ExternalIndicatorConfiguration> {

    ExternalIndicatorConfigurationDTO readConfigurationForInstitution(Integer institutionId);

    ExternalIndicatorConfigurationDTO readConfigurationForDocument(Integer documentId);

    void updateConfiguration(ExternalIndicatorConfigurationDTO configuration,
                             Integer institutionId);
}
