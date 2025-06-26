package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class OrgUnitEditCheckAspect {

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;


    @Around("@annotation(rs.teslaris.core.annotation.OrgUnitEditCheck)")
    public Object checkOrganisationUnitEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = AspectUtil.getRequest();
        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(tokenValue));
        var organisationUnitId = Integer.parseInt(attributeMap.get("organisationUnitId"));

        switch (role) {
            case ADMIN:
                break;
            case INSTITUTIONAL_EDITOR:
                var userId = tokenUtil.extractUserIdFromToken(tokenValue);
                var editableOUs =
                    organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                        userService.getUserOrganisationUnitId(userId));
                if (!editableOUs.contains(organisationUnitId)) {
                    throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
        }

        return joinPoint.proceed();
    }
}
