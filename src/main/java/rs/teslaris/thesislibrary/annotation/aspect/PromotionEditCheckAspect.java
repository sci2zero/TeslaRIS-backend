package rs.teslaris.thesislibrary.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.aspect.AspectUtil;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.service.interfaces.PromotionService;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEditCheckAspect {

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final PromotionService promotionService;


    @Around("@annotation(rs.teslaris.thesislibrary.annotation.PromotionEditAndUsageCheck)")
    public Object checkPromotionEditAndUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var promotionId = Integer.parseInt(attributeMap.get("promotionId"));
        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case PROMOTION_REGISTRY_ADMINISTRATOR:
                if (!organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                        userService.getUserOrganisationUnitId(userId))
                    .contains(promotionService.findOne(promotionId).getInstitution().getId())) {
                    throw new CantEditException("Unauthorised to edit or use this promotion.");
                }
                break;
            default:
                throw new CantEditException("Unauthorised role.");
        }

        return joinPoint.proceed();
    }
}
