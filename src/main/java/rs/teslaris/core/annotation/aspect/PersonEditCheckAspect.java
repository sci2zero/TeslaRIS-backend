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
import rs.teslaris.core.model.user.UserRole;
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

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PersonEditCheck)")
    public Object checkPersonEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var bearerToken = request.getHeader("Authorization");
        var tokenValue = bearerToken.split(" ")[1];
        var attributeMap = (Map<String, String>) request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        var personId = Integer.parseInt(attributeMap.get("personId"));

        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
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

        return joinPoint.proceed();
    }
}
