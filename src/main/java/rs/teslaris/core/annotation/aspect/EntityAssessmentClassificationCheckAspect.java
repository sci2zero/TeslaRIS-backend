package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.assessment.service.interfaces.EntityAssessmentClassificationService;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class EntityAssessmentClassificationCheckAspect {

    private final UserService userService;

    private final EntityAssessmentClassificationService entityAssessmentClassificationService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.EntityClassificationEditCheck)")
    public Object checkEntityAssessmentClassificationEdit(ProceedingJoinPoint joinPoint)
        throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var entityAssessmentClassificationId =
            Integer.parseInt(attributeMap.get("entityAssessmentClassificationId"));
        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        var user = userService.findOne(userId);
        var classification =
            entityAssessmentClassificationService.findOne(entityAssessmentClassificationId);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case COMMISSION:
                if (!classification.getCommission().getId().equals(user.getCommission().getId())) {
                    throw new CantEditException("unauthorizedClassificationEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedClassificationEditAttemptMessage");
        }

        return joinPoint.proceed();
    }
}
