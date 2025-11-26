package rs.teslaris.core.controller;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.commontypes.MaintenanceInformationDTO;
import rs.teslaris.core.service.interfaces.commontypes.ApplicationConfigurationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/app-configuration")
@RequiredArgsConstructor
public class ApplicationConfigurationController {

    private final ApplicationConfigurationService applicationConfigurationService;

    private final JwtUtil tokenUtil;


    @GetMapping("/maintenance/check")
    public boolean getIsApplicationInMaintenanceMode() {
        return applicationConfigurationService.isApplicationInMaintenanceMode();
    }

    @GetMapping("/maintenance/next")
    public MaintenanceInformationDTO getNextScheduledMaintenance() {
        return applicationConfigurationService.getNextScheduledMaintenance();
    }

    @PostMapping("/maintenance/schedule")
    @Idempotent
    @PreAuthorize("hasAuthority('CONFIGURE_APP_SETTINGS')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleMaintenance(@RequestParam("timestamp") LocalDateTime timestamp,
                                    @RequestParam("approximateEndMoment")
                                    String approximateEndMoment,
                                    @RequestHeader("Authorization") String bearerToken) {
        applicationConfigurationService.scheduleMaintenanceMode(timestamp, approximateEndMoment,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/maintenance/turn-on")
    @PreAuthorize("hasAuthority('CONFIGURE_APP_SETTINGS')")
    public void turnOnMaintenanceMode() {
        applicationConfigurationService.turnOnMaintenanceMode();
    }

    @PatchMapping("/maintenance/turn-off")
    @PreAuthorize("hasAuthority('CONFIGURE_APP_SETTINGS')")
    public void turnOffMaintenanceMode() {
        applicationConfigurationService.turnOffMaintenanceMode();
    }
}
