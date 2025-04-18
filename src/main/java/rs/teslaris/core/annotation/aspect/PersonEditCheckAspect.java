package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class PersonEditCheckAspect {

    private final UserService userService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PersonEditCheck)")
    public Object checkPersonEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = AspectUtil.getRequest();
        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(tokenValue));
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        var annotation = AspectUtil.getMethod(joinPoint).getAnnotation(PersonEditCheck.class);

        if (annotation.value().equalsIgnoreCase("CREATE")) {
            validateCreatePermission(role, joinPoint, userId);
            return joinPoint.proceed();
        }

        if (annotation.value().equalsIgnoreCase("ADD_EMPLOYMENT") &&
            role.equals(UserRole.INSTITUTIONAL_EDITOR)) {
            return joinPoint.proceed();
        }

        var personId = Integer.parseInt(attributeMap.get("personId"));
        validateEditPermission(role, userId, personId);

        return joinPoint.proceed();
    }

    private void validateCreatePermission(UserRole role, ProceedingJoinPoint joinPoint,
                                          Integer userId) {
        if (role != UserRole.INSTITUTIONAL_EDITOR) {
            return;
        }

        var researcherEmploymentInstitution = getEmploymentInstitutionFromDTO(joinPoint);
        var editorInstitution = userService.getUserOrganisationUnitId(userId);
        var allPossibleInstitutions =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(editorInstitution);

        if (!allPossibleInstitutions.contains(researcherEmploymentInstitution)) {
            throw new CantEditException("unauthorizedPersonEditAttemptMessage");
        }
    }

    private void validateEditPermission(UserRole role, Integer userId, Integer personId) {
        switch (role) {
            case ADMIN:
                return;
            case RESEARCHER:
                if (!userService.isUserAResearcher(userId, personId)) {
                    throw new CantEditException("unauthorizedPersonEditAttemptMessage");
                }
                break;
            case INSTITUTIONAL_EDITOR:
                if (!personService.isPersonEmployedInOrganisationUnit(personId,
                    userService.getUserOrganisationUnitId(userId))) {
                    throw new CantEditException("unauthorizedPersonEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedPersonEditAttemptMessage");
        }
    }

    private Integer getEmploymentInstitutionFromDTO(ProceedingJoinPoint joinPoint) {
        var personDTO = (BasicPersonDTO) joinPoint.getArgs()[0];
        return personDTO.getOrganisationUnitId();
    }
}
