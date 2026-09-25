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
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.service.interfaces.commontypes.CrisContextInformationService;

@RestController
@RequestMapping("/api/cris-context-information")
@RequiredArgsConstructor
public class CrisContextInformationController {

    private final CrisContextInformationService crisContextInformationService;


    @GetMapping
    public CrisContextInformationDTO fetchConfigurationForSystem() {
        return crisContextInformationService.readConfigurationForSystem();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('SAVE_MODULE_ACCESS_CONFIGURATION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrisContextInformationDTO saveConfigurationForSystem(
        @RequestBody @Valid CrisContextInformationDTO configuration) {
        return crisContextInformationService.saveConfiguration(configuration);
    }
}
