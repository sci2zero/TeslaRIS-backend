package rs.teslaris.core.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.EntityIndicatorEditCheck;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.service.interfaces.EntityIndicatorService;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/entity-indicator")
public class EntityIndicatorController {

    private static UserService userService;
    private static JwtUtil tokenUtil;
    private final EntityIndicatorService entityIndicatorService;


    @Autowired
    public EntityIndicatorController(EntityIndicatorService entityIndicatorService,
                                     UserService userService, JwtUtil tokenUtil) {
        this.entityIndicatorService = entityIndicatorService;
        EntityIndicatorController.userService = userService;
        EntityIndicatorController.tokenUtil = tokenUtil;
    }

    public static Object fetchIndicators(HttpServletRequest request,
                                         String bearerToken,
                                         String fingerprintCookie,
                                         Function<AccessLevel, Object> documentServiceFunction) {
        if (Objects.isNull(bearerToken)) {
            return documentServiceFunction.apply(AccessLevel.OPEN);
        }

        var token = bearerToken.split(" ")[1];
        var userDetails =
            userService.loadUserByUsername(tokenUtil.extractUsernameFromToken(token));

        if (!tokenUtil.validateToken(token, userDetails, fingerprintCookie)) {
            return ErrorResponseUtil.buildUnauthorisedResponse(request,
                "unauthorisedToViewIndicatorMessage");
        }

        var role = tokenUtil.extractUserRoleFromToken(token);
        return switch (UserRole.valueOf(role)) {
            case ADMIN -> documentServiceFunction.apply(AccessLevel.ADMIN_ONLY);
            case RESEARCHER, INSTITUTIONAL_EDITOR, COMMISSION, VICE_DEAN_FOR_SCIENCE ->
                documentServiceFunction.apply(AccessLevel.CLOSED);
        };
    }

    @PatchMapping(value = "/add-proof/{entityIndicatorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_INDICATOR_PROOFS')")
    @Idempotent
    @EntityIndicatorEditCheck
    public DocumentFileResponseDTO addEntityIndicatorProof(
        @ModelAttribute @Valid DocumentFileDTO proof,
        @PathVariable Integer entityIndicatorId) {
        return entityIndicatorService.addEntityIndicatorProof(proof, entityIndicatorId);
    }

    @PatchMapping(value = "/update-proof/{entityIndicatorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_INDICATOR_PROOFS')")
    @EntityIndicatorEditCheck
    @Idempotent
    public DocumentFileResponseDTO updateEntityIndicatorProof(
        @ModelAttribute @Valid DocumentFileDTO proof) {
        return entityIndicatorService.updateEntityIndicatorProof(proof);
    }

    @DeleteMapping("/{entityIndicatorId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_INDICATOR_PROOFS')")
    @EntityIndicatorEditCheck
    public void deleteEntityIndicatorProof(@PathVariable Integer entityIndicatorId,
                                           @PathVariable Integer proofId) {
        entityIndicatorService.deleteEntityIndicatorProof(entityIndicatorId, proofId);
    }

    @DeleteMapping("/{entityIndicatorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_INDICATOR')")
    @EntityIndicatorEditCheck
    public void deleteEntityIndicator(@PathVariable Integer entityIndicatorId) {
        entityIndicatorService.deleteEntityIndicator(entityIndicatorId);
    }
}
