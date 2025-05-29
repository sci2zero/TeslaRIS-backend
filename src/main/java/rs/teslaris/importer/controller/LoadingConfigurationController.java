package rs.teslaris.importer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;

@RestController
@RequestMapping("/api/loading-configuration")
@RequiredArgsConstructor
@Traceable
public class LoadingConfigurationController {

    private final JwtUtil tokenUtil;

    private final LoadingConfigurationService loadingConfigurationService;

    private final UserService userService;


    @PostMapping
    @PreAuthorize("hasAuthority('SAVE_LOADING_CONFIG')")
    public void saveLoadingConfiguration(@RequestHeader(value = "Authorization") String bearerToken,
                                         @RequestParam Integer institutionId,
                                         @RequestBody @Valid
                                         LoadingConfigurationDTO loadingConfigurationDTO) {
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);
        if (userRole.equals(UserRole.ADMIN.name())) {
            loadingConfigurationService.saveLoadingConfiguration(institutionId,
                loadingConfigurationDTO);
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            var userId = tokenUtil.extractUserIdFromToken(bearerToken);
            loadingConfigurationService.saveLoadingConfiguration(
                userService.getUserOrganisationUnitId(userId), loadingConfigurationDTO);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    public LoadingConfigurationDTO readConfigurationForUser(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return loadingConfigurationService.getLoadingConfigurationForUser(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }
}
