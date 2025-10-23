package rs.teslaris.reporting.controller.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.reporting.dto.configuration.DigitalLibraryChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.DigitalLibraryChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/digital-library")
@RequiredArgsConstructor
public class DLChartDisplayConfigurationController {

    private final DigitalLibraryChartsDisplayConfigurationService
        digitalLibraryChartsDisplayConfigurationService;


    @GetMapping("/{organisationUnitId}")
    public DigitalLibraryChartDisplaySettingsDTO getChartDisplaySettingsForOrganisationUnit(
        @PathVariable Integer organisationUnitId) {
        return digitalLibraryChartsDisplayConfigurationService.getDisplaySettingsForInstitution(
            organisationUnitId);
    }
}
