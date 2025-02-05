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

        var bearerToken = request.getHeader("Authorization");
        var tokenValue = bearerToken.split(" ")[1];
        var attributeMap = (Map<String, String>) request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
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
            case RESEARCHER, INSTITUTIONAL_EDITOR:
                throw new CantEditException(
                    "unauthorizedCommissionEditAttemptMessage");
            default:
                throw new CantEditException("unauthorizedCommissionEditAttemptMessage");
        }

        return joinPoint.proceed();
    }
}
