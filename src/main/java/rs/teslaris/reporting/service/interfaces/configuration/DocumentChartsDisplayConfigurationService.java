package rs.teslaris.reporting.service.interfaces.configuration;

import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;

@Service
public interface DocumentChartsDisplayConfigurationService {

    DocumentChartDisplaySettingsDTO getDisplaySettingsForDocument(Integer documentId);

    void saveDocumentDisplaySettings(Integer institutionId,
                                     DocumentChartDisplaySettingsDTO settings);
}
