package rs.teslaris.core.controller.commontypes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.FeatureModuleTogglesDTO;
import rs.teslaris.core.service.interfaces.commontypes.FeatureModuleTogglesService;

@RestController
@RequestMapping("/api/feature-module-toggles")
@RequiredArgsConstructor
public class FeatureModuleTogglesController {

    private final FeatureModuleTogglesService featureModuleTogglesService;


    @GetMapping
    public FeatureModuleTogglesDTO fetchConfigurationForSystem() {
        return featureModuleTogglesService.readConfigurationForSystem();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('SAVE_MODULE_ACCESS_CONFIGURATION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public FeatureModuleTogglesDTO saveConfigurationForSystem(
        @RequestBody @Valid FeatureModuleTogglesDTO configuration) {
        return featureModuleTogglesService.saveConfiguration(configuration);
    }
}
