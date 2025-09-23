package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
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
        var annotation = AspectUtil.getMethod(joinPoint).getAnnotation(OrgUnitEditCheck.class);

        List<Integer> organisationUnitIds = new ArrayList<>();
        if (attributeMap.containsKey("organisationUnitId")) {
            organisationUnitIds.add(Integer.parseInt(attributeMap.get("organisationUnitId")));
        } else if (attributeMap.containsKey("leftOrganisationUnitId") &&
            attributeMap.containsKey("rightOrganisationUnitId")) {
            if (!annotation.value().equalsIgnoreCase("MERGE")) {
                organisationUnitIds.add(
                    Integer.parseInt(attributeMap.get("leftOrganisationUnitId")));
            }
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
            validateAccessPermissions(role, tokenValue, organisationUnitId, annotation);
        }

        return joinPoint.proceed();
    }

    private void validateAccessPermissions(UserRole role, String tokenValue,
                                           Integer organisationUnitId,
                                           OrgUnitEditCheck annotation) {
        switch (role) {
            case ADMIN:
                break;
            case INSTITUTIONAL_EDITOR, INSTITUTIONAL_LIBRARIAN:
                var userId = tokenUtil.extractUserIdFromToken(tokenValue);
                var editableOUs =
                    organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                        userService.getUserOrganisationUnitId(userId));
                if (!editableOUs.contains(organisationUnitId)) {
                    throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
                }

                if (role.equals(UserRole.INSTITUTIONAL_LIBRARIAN) &&
                    !annotation.value().equalsIgnoreCase("LIBRARY_OPERATIONS")) {
                    throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
                }

                break;
            default:
                throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
        }
    }
}
