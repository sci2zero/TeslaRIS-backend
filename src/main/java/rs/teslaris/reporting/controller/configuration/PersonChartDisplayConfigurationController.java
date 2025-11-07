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
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.service.interfaces.configuration.PersonChartsDisplayConfigurationService;

@RestController
@RequestMapping("/api/chart-display-configuration/person")
@RequiredArgsConstructor
public class PersonChartDisplayConfigurationController {

    private final PersonChartsDisplayConfigurationService personChartsDisplayConfigurationService;


    @GetMapping("/{personId}")
    public PersonChartDisplaySettingsDTO getChartDisplaySettingsForPerson(
        @PathVariable Integer personId) {
        return personChartsDisplayConfigurationService.getDisplaySettingsForPerson(personId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_CHART_DISPLAY_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void savePersonChartDisplaySettings(@PathVariable Integer organisationUnitId,
                                               @RequestBody
                                               PersonChartDisplaySettingsDTO settings) {
        personChartsDisplayConfigurationService.savePersonDisplaySettings(organisationUnitId,
            settings);
    }
}
