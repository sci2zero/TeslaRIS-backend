package rs.teslaris.core.controller.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.PublicConfigurationDTO;
import rs.teslaris.core.service.interfaces.commontypes.PublicConfigurationService;

@RestController
@RequestMapping("/api/public-configuration")
@RequiredArgsConstructor
@Traceable
public class PublicConfigurationController {

    private final PublicConfigurationService publicConfigurationService;


    @GetMapping
    public PublicConfigurationDTO readPublicConfiguration() {
        return publicConfigurationService.readPublicConfiguration();
    }
}
