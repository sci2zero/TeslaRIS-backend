package rs.teslaris.core.controller.document;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.PrepopulatedMetadataDTO;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.MetadataPrepopulationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/metadata-prepopulation")
@RequiredArgsConstructor
public class MetadataPrepopulationController {

    private final MetadataPrepopulationService metadataPrepopulationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping
    @PreAuthorize("hasAuthority('HARVEST_IDF_METADATA')")
    public PrepopulatedMetadataDTO getMetadataForDoi(@RequestParam String doi,
                                                     @RequestHeader("Authorization")
                                                     String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.RESEARCHER.name())) {
            return metadataPrepopulationService.fetchBibTexDataForDoi(doi,
                userService.getPersonIdForUser(tokenUtil.extractUserIdFromToken(bearerToken)));
        }

        return metadataPrepopulationService.fetchBibTexDataForDoi(doi, null);
    }
}
