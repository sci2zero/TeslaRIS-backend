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
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class CommissionEditCheckAspect {

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @Around("@annotation(rs.teslaris.core.annotation.CommissionEditCheck)")
    public Object checkCommissionEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var commissionId = Integer.parseInt(attributeMap.get("commissionId"));
        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case COMMISSION:
                if (!userService.getUserProfile(userId).getCommissionId().equals(commissionId)) {
                    throw new CantEditException(
                        "unauthorizedCommissionEditAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedCommissionEditAttemptMessage");
        }

        return joinPoint.proceed();
    }
}
