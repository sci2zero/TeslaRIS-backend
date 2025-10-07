package rs.teslaris.reporting.controller.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.reporting.dto.configuration.FullChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.BaseChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/base")
@RequiredArgsConstructor
public class BaseChartDisplayConfigurationController {

    private final BaseChartsDisplayConfigurationService baseChartsDisplayConfigurationService;


    @GetMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    public FullChartDisplaySettingsDTO getFullChartDisplaySettings(
        @PathVariable Integer organisationUnitId) {
        return baseChartsDisplayConfigurationService.getSavedConfigurationForOU(organisationUnitId);
    }
}
