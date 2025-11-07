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
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.OUChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/organisation-unit")
@RequiredArgsConstructor
public class OUChartDisplayConfigurationController {

    private final OUChartsDisplayConfigurationService ouChartsDisplayConfigurationService;


    @GetMapping("/{organisationUnitId}")
    public OUChartDisplaySettingsDTO getChartDisplaySettingsForOrganisationUnit(
        @PathVariable Integer organisationUnitId) {
        return ouChartsDisplayConfigurationService.getDisplaySettingsForOrganisationUnit(
            organisationUnitId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void saveOrganisationUnitChartDisplaySettings(@PathVariable Integer organisationUnitId,
                                                         @RequestBody
                                                         OUChartDisplaySettingsDTO settings) {
        ouChartsDisplayConfigurationService.saveOrganisationUnitDisplaySettings(organisationUnitId,
            settings);
    }
}
