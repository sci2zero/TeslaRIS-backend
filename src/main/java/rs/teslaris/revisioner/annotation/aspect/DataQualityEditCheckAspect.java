package rs.teslaris.revisioner.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import rs.teslaris.core.annotation.aspect.AspectUtil;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;

@Aspect
@Component
@RequiredArgsConstructor
public class DataQualityEditCheckAspect {

    private final JwtUtil tokenUtil;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;


    @Around("@annotation(rs.teslaris.revisioner.annotation.DataQualityEditCheck)")
    public Object checkDataQualityEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = AspectUtil.getRequest();
        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(tokenValue));
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);

        List<DataQualityAssessmentIndex> assessmentIndexes = new ArrayList<>();

        if (attributeMap.containsKey("entityType") && attributeMap.containsKey("entityId")) {
            var entityType = attributeMap.get("entityType");
            var entityId = Integer.parseInt(attributeMap.get("entityId"));
            assessmentIndexes.addAll(
                dataQualityAssessmentIndexRepository.findByEntityTypeAndEntityIdAndIsLatestTrue(
                    entityType, entityId)
            );

            if (assessmentIndexes.isEmpty()) {
                throw new NotFoundException(
                    "Entity " + entityType + " with ID " + entityId + " does not exist.");
            }
        } else if (attributeMap.containsKey("assessmentId")) {
            dataQualityAssessmentIndexRepository.findByDatabaseId(
                    Integer.parseInt(attributeMap.get("assessmentId")))
                .ifPresent(assessmentIndexes::add);
        }

        if (assessmentIndexes.isEmpty()) {
            throw new IllegalArgumentException(
                "Missing assessment identifiers."); // should never happen in prod, only for testing
        }

        validateAccessPermissions(role, userId, assessmentIndexes.getFirst());

        return joinPoint.proceed();
    }

    private void validateAccessPermissions(UserRole role, Integer userId,
                                           DataQualityAssessmentIndex assessment) {
        switch (role) {
            case ADMIN:
                break;
            case INSTITUTIONAL_EDITOR, VICE_DEAN_FOR_SCIENCE:
                // A unit answers for everything below it, and records are indexed under the unit
                // they belong to rather than its ancestors - so the user's sub-hierarchy is what
                // decides, otherwise an editor of a faculty could not open a record of its own
                // department.
                var scopeIds = organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                    userService.getUserOrganisationUnitId(userId));

                if (Collections.disjoint(assessment.getOrganisationUnitIds(), scopeIds)) {
                    throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
                }

                break;
            default:
                throw new CantEditException("unauthorizedOrgUnitEditAttemptMessage");
        }
    }
}
