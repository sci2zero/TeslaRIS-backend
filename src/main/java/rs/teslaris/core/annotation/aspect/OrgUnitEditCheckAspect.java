package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
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

        List<Integer> organisationUnitIds = new ArrayList<>();
        if (attributeMap.containsKey("organisationUnitId")) {
            organisationUnitIds.add(Integer.parseInt(attributeMap.get("organisationUnitId")));
        } else if (attributeMap.containsKey("leftOrganisationUnitId") &&
            attributeMap.containsKey("rightOrganisationUnitId")) {
            organisationUnitIds.add(Integer.parseInt(attributeMap.get("leftOrganisationUnitId")));
            organisationUnitIds.add(Integer.parseInt(attributeMap.get("rightOrganisationUnitId")));
        } else if (attributeMap.containsKey("relationId")) {
            // TODO: Check if we should check both source and target OU IDs!
            organisationUnitIds.add(organisationUnitService.findOrganisationUnitsRelationById(
                    Integer.parseInt(attributeMap.get("relationId"))).getSourceOrganisationUnit()
                .getId());
        } else {
            throw new IllegalArgumentException(
                "Missing OU identifiers."); // should never happen in prod, only for testing
        }

        for (var organisationUnitId : organisationUnitIds) {
            validateAccessPermissions(role, tokenValue, organisationUnitId);
        }

        return joinPoint.proceed();
    }

    private void validateAccessPermissions(UserRole role, String tokenValue,
                                           Integer organisationUnitId) {
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
    }
}
