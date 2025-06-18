package rs.teslaris.assessment.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.assessment.service.interfaces.indicator.EntityIndicatorService;
import rs.teslaris.core.annotation.aspect.AspectUtil;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class EntityIndicatorEditCheckAspect {

    private final EntityIndicatorService entityIndicatorService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.assessment.annotation.EntityIndicatorEditCheck)")
    public Object checkEntityIndicatorEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var entityIndicatorId = Integer.parseInt(attributeMap.get("entityIndicatorId"));
        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case RESEARCHER, COMMISSION:
                if (!entityIndicatorService.isUserTheOwnerOfEntityIndicator(userId,
                    entityIndicatorId)) {
                    throw new CantEditException(
                        "unauthorizedEntityIndicatorEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedEntityIndicatorEditAttemptMessage");
        }

        return joinPoint.proceed();
    }
}
