package rs.teslaris.project.controller.identifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.controller.identifier.EntityIdentifierController;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.project.annotation.ProjectEditCheck;
import rs.teslaris.project.dto.identifier.ProjectIdentifierDTO;
import rs.teslaris.project.service.interfaces.identifier.ProjectIdentifierService;

@RestController
@RequestMapping("/api/project-identifier")
@RequiredArgsConstructor
@Traceable
public class ProjectIdentifierController {

    private final ProjectIdentifierService projectIdentifierService;


    @GetMapping("/{projectId}")
    public Object getProjectIdentifiers(HttpServletRequest request,
                                    @PathVariable Integer projectId,
                                    @RequestHeader(value = "Authorization", required = false)
                                    String bearerToken,
                                    @CookieValue(value = "jwt-security-fingerprint", required = false)
                                    String fingerprintCookie) {
        return EntityIdentifierController.fetchIdentifiers(
            request,
            bearerToken,
            fingerprintCookie,
            accessLevel -> projectIdentifierService.getIdentifiersForProject(projectId, accessLevel)
        );
    }

    @PostMapping("/{projectId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PROJECT_IDENTIFIERS')")
    @ProjectEditCheck
    @Idempotent
    @Transactional
    public EntityIdentifierResponseDTO createProjectIdentifier(
        @PathVariable Integer projectId,
        @RequestBody @Valid ProjectIdentifierDTO projectIdentifierDTO) {
        // The path ID is the one the permission check saw, so it wins over the body.
        projectIdentifierDTO.setProjectId(projectId);

        return EntityIdentifierConverter.toDTO(
            projectIdentifierService.createProjectIdentifier(projectIdentifierDTO));
    }

    @PutMapping("/{projectId}/{entityIdentifierId}")
    @PreAuthorize("hasAuthority('EDIT_PROJECT_IDENTIFIERS')")
    @ProjectEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProjectIdentifier(
        @PathVariable Integer projectId,
        @PathVariable Integer entityIdentifierId,
        @RequestBody @Valid ProjectIdentifierDTO projectIdentifierDTO) {
        projectIdentifierDTO.setProjectId(projectId);

        projectIdentifierService.updateProjectIdentifier(entityIdentifierId, projectIdentifierDTO);
    }
}
