package rs.teslaris.core.controller.identifier;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.identifier.EntityIdentifierService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/entity-identifier")
@RequiredArgsConstructor
@Traceable
public class EntityIdentifierController {

    private static UserService userService;

    private static JwtUtil tokenUtil;

    private final EntityIdentifierService entityIdentifierService;


    @Autowired
    public EntityIdentifierController(EntityIdentifierService entityIdentifierService,
                                      UserService userService, JwtUtil tokenUtil) {
        this.entityIdentifierService = entityIdentifierService;
        EntityIdentifierController.userService = userService;
        EntityIdentifierController.tokenUtil = tokenUtil;
    }

    public static Object fetchIdentifiers(HttpServletRequest request,
                                          String bearerToken,
                                          String fingerprintCookie,
                                          Function<AccessLevel, Object> entityServiceFunction) {
        if (Objects.isNull(bearerToken)) {
            return entityServiceFunction.apply(AccessLevel.OPEN);
        }

        var token = bearerToken.split(" ")[1];
        var userDetails =
            userService.loadUserByUsername(tokenUtil.extractUsernameFromToken(token));

        if (!tokenUtil.validateToken(token, userDetails, fingerprintCookie)) {
            return ErrorResponseUtil.buildUnauthorisedResponse(request,
                "unauthorisedToViewIdentifierMessage");
        }

        var role = tokenUtil.extractUserRoleFromToken(token);
        return switch (UserRole.valueOf(role)) {
            case ADMIN -> entityServiceFunction.apply(AccessLevel.ADMIN_ONLY);
            case RESEARCHER, INSTITUTIONAL_EDITOR,
                 COMMISSION, VICE_DEAN_FOR_SCIENCE,
                 INSTITUTIONAL_LIBRARIAN, HEAD_OF_LIBRARY,
                 PROMOTION_REGISTRY_ADMINISTRATOR ->
                entityServiceFunction.apply(AccessLevel.CLOSED);
        };
    }

    @DeleteMapping("/{entityIdentifierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_IDENTIFIER')")
    public void deleteEntityIdentifier(@PathVariable Integer entityIdentifierId) {
        entityIdentifierService.deleteEntityIdentifier(entityIdentifierId);
    }
}
