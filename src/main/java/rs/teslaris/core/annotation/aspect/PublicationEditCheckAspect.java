package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class PublicationEditCheckAspect {

    private final DocumentPublicationService documentPublicationService;

    private final PersonService personService;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PublicationEditCheck)")
    public Object checkApiKey(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        Method method = getMethod(joinPoint);
        PublicationEditCheck annotation = method.getAnnotation(PublicationEditCheck.class);

        String tokenValue = request.getHeader("Authorization").split(" ")[1];
        Map<String, String> attributeMap = (Map<String, String>) request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        String role = tokenUtil.extractUserRoleFromToken(tokenValue);
        int personId = userService.getPersonIdForUser(tokenUtil.extractUserIdFromToken(tokenValue));

        List<Integer> contributors = getContributors(annotation, attributeMap, joinPoint);

        checkPermission(role, personId, contributors);

        return joinPoint.proceed();
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        var signature = joinPoint.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("Aspect should be applied to a method.");
        }
        return ((MethodSignature) signature).getMethod();
    }

    private List<Integer> getContributors(PublicationEditCheck annotation,
                                          Map<String, String> attributeMap,
                                          ProceedingJoinPoint joinPoint) {
        if (annotation.value().equalsIgnoreCase("CREATE")) {
            return getContributorsFromDTO(joinPoint);
        } else {
            return getContributorsFromDatabase(attributeMap);
        }
    }

    private List<Integer> getContributorsFromDTO(ProceedingJoinPoint joinPoint) {
        var publicationDTO = (DocumentDTO) joinPoint.getArgs()[0];
        return publicationDTO.getContributions().stream().map(PersonContributionDTO::getPersonId)
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Integer> getContributorsFromDatabase(Map<String, String> attributeMap) {
        int publicationId = Integer.parseInt(attributeMap.get("documentId"));
        return documentPublicationService.getContributorIds(publicationId);
    }

    private void checkPermission(String role, int userId, List<Integer> contributors) {
        UserRole userRole = UserRole.valueOf(role);
        switch (userRole) {
            case ADMIN:
                break;
            case RESEARCHER:
                if (!contributors.contains(userId)) {
                    throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
                }
                break;
            case INSTITUTIONAL_EDITOR:
                if (contributors.stream().noneMatch(
                    personId -> personService.isPersonEmployedInOrganisationUnit(personId,
                        userService.getUserOrganisationUnitId(userId)))) {
                    throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
        }
    }
}
