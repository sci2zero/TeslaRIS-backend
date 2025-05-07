package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.DocumentFileNavigationDTO;
import rs.teslaris.core.dto.commontypes.EntityNavigationDTO;
import rs.teslaris.core.service.interfaces.commontypes.NavigationBackwardCompatibilityService;

@RestController
@RequestMapping("/api/legacy-navigation")
@RequiredArgsConstructor
public class NavigationBackwardCompatibilityController {

    private final NavigationBackwardCompatibilityService navigationBackwardCompatibilityService;


    @GetMapping("/entity-landing-page/{oldId}")
    public EntityNavigationDTO getBackwardCompatibleId(@PathVariable Integer oldId,
                                                       @RequestParam(defaultValue = "NONE")
                                                       String source,
                                                       @RequestParam(defaultValue = "NONE")
                                                       String language) {
        var response =
            navigationBackwardCompatibilityService.readResourceByOldId(oldId, source, language);
        return new EntityNavigationDTO(response.a, response.b);
    }

    @GetMapping("/document-file/{oldServerFilename}")
    public DocumentFileNavigationDTO getBackwardCompatibleFilename(
        @PathVariable String oldServerFilename, @RequestParam(defaultValue = "NONE") String source,
        @RequestParam(defaultValue = "NONE") String language) {
        var response =
            navigationBackwardCompatibilityService.readDocumentFileByOldId(oldServerFilename,
                source, language);
        return new DocumentFileNavigationDTO(response.a, response.b);
    }
}
