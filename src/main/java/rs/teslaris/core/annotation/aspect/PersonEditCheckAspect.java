package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
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

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PersonEditCheck)")
    public Object checkPersonEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getRequest();
        var tokenValue = extractToken(request);
        var attributeMap = getUriVariables(request);

        var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(tokenValue));
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        var annotation = AspectUtil.getMethod(joinPoint).getAnnotation(PersonEditCheck.class);

        if (annotation.value().equalsIgnoreCase("CREATE")) {
            validateCreatePermission(role, joinPoint, userId);
            return joinPoint.proceed();
        }

        var personId = Integer.parseInt(attributeMap.get("personId"));
        validateEditPermission(role, userId, personId);

        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes()))
            .getRequest();
    }

    private String extractToken(HttpServletRequest request) {
        var bearerToken = request.getHeader("Authorization");
        return bearerToken.split(" ")[1];
    }

    private Map<String, String> getUriVariables(HttpServletRequest request) {
        return (Map<String, String>) request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    private void validateCreatePermission(UserRole role, ProceedingJoinPoint joinPoint,
                                          Integer userId) {
        if (role != UserRole.INSTITUTIONAL_EDITOR) {
            return;
        }

        var researcherEmploymentInstitution = getEmploymentInstitutionFromDTO(joinPoint);
        var editorInstitution = userService.getUserOrganisationUnitId(userId);
        var allPossibleInstitutions =
            organisationUnitsRelationRepository.getSubOUsRecursive(editorInstitution);
        allPossibleInstitutions.add(editorInstitution);

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
