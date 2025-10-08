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
import rs.teslaris.reporting.dto.configuration.FullChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.BaseChartsDisplayConfigurationService;
import rs.teslaris.reporting.service.interfaces.configuration.DocumentChartsDisplayConfigurationService;
import rs.teslaris.reporting.service.interfaces.configuration.OUChartsDisplayConfigurationService;
import rs.teslaris.reporting.service.interfaces.configuration.PersonChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/base")
@RequiredArgsConstructor
public class BaseChartDisplayConfigurationController {

    private final BaseChartsDisplayConfigurationService baseChartsDisplayConfigurationService;

    private final PersonChartsDisplayConfigurationService personChartsDisplayConfigurationService;

    private final OUChartsDisplayConfigurationService ouChartsDisplayConfigurationService;

    private final DocumentChartsDisplayConfigurationService
        documentChartsDisplayConfigurationService;


    @GetMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    public FullChartDisplaySettingsDTO getFullChartDisplaySettings(
        @PathVariable Integer organisationUnitId) {
        return baseChartsDisplayConfigurationService.getSavedConfigurationForOU(organisationUnitId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void saveFullChartDisplaySettings(@PathVariable Integer organisationUnitId,
                                             @RequestBody
                                             FullChartDisplaySettingsDTO settings) {
        personChartsDisplayConfigurationService.savePersonDisplaySettings(organisationUnitId,
            settings.getPersonChartDisplaySettings());
        ouChartsDisplayConfigurationService.saveOrganisationUnitDisplaySettings(organisationUnitId,
            settings.getOuChartDisplaySettings());
        documentChartsDisplayConfigurationService.saveDocumentDisplaySettings(organisationUnitId,
            settings.getDocumentChartDisplaySettings());
    }
}
