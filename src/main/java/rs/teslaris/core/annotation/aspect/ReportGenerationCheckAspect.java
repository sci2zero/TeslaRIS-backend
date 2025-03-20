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
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class ReportGenerationCheckAspect {

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final UserRepository userRepository;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;


    @Around("@annotation(rs.teslaris.core.annotation.ReportGenerationCheck)")
    public Object checkReportGeneration(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var tokenValue = AspectUtil.extractToken(request);
        String[] commissionIdStrings = request.getParameterValues("commissionId");

        var role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        switch (UserRole.valueOf(role)) {
            case ADMIN:
                break;
            case VICE_DEAN_FOR_SCIENCE:
                if (!isAccessGranted(userId, commissionIdStrings)) {
                    throw new CantEditException(
                        "unauthorizedReportGenerationAttemptMessage");
                }
                break;
            default:
                throw new CantEditException("unauthorizedReportGenerationAttemptMessage");
        }

        return joinPoint.proceed();
    }

    public boolean isAccessGranted(Integer userId, String[] commissionIdStrings) {
        var topLevelInstitutionId = userService.getUserOrganisationUnitId(userId);
        var possibleInstitutionsForGeneration =
            organisationUnitsRelationRepository.getSubOUsRecursive(topLevelInstitutionId);
        possibleInstitutionsForGeneration.add(topLevelInstitutionId);
        for (var commissionId : commissionIdStrings) {
            var commissionOUId =
                userRepository.findOUIdForCommission(Integer.parseInt(commissionId));
            if (Objects.isNull(commissionOUId) ||
                !possibleInstitutionsForGeneration.contains(commissionOUId)) {
                return false;
            }
        }

        return true;
    }
}
