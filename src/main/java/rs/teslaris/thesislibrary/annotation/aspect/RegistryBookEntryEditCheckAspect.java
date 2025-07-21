package rs.teslaris.thesislibrary.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.aspect.AspectUtil;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookService;

@Aspect
@Component
@RequiredArgsConstructor
public class RegistryBookEntryEditCheckAspect {

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final RegistryBookService registryBookService;


    @Around("@annotation(rs.teslaris.thesislibrary.annotation.RegistryBookEntryEditCheck)")
    public Object checkRegistryBookEntryEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var registryBookEntryId = Integer.parseInt(attributeMap.get("registryBookEntryId"));
        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case PROMOTION_REGISTRY_ADMINISTRATOR:
                if (!organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                        userService.getUserOrganisationUnitId(userId))
                    .contains(registryBookService.findOne(registryBookEntryId)
                        .getDissertationInformation().getOrganisationUnit().getId())) {
                    throw new CantEditException("Unauthorised to edit or use this promotion.");
                }
                break;
            default:
                throw new CantEditException("Unauthorised role.");
        }

        return joinPoint.proceed();
    }
}
