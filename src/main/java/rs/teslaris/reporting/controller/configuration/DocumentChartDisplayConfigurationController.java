package rs.teslaris.reporting.controller.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.DocumentChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/document")
@RequiredArgsConstructor
public class DocumentChartDisplayConfigurationController {

    private final DocumentChartsDisplayConfigurationService
        documentChartsDisplayConfigurationService;


    @GetMapping("/{documentId}")
    public DocumentChartDisplaySettingsDTO getChartDisplaySettingsForDocument(
        @PathVariable Integer documentId) {
        return documentChartsDisplayConfigurationService.getDisplaySettingsForDocument(documentId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void saveDocumentChartDisplaySettings(@PathVariable Integer organisationUnitId,
                                                 @RequestBody
                                                 DocumentChartDisplaySettingsDTO settings) {
        documentChartsDisplayConfigurationService.saveDocumentDisplaySettings(organisationUnitId,
            settings);
    }
}
