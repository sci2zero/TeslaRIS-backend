package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.PublicationMergeCheck;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@RequiredArgsConstructor
public class PublicationMergeCheckAspect {

    private final DocumentPublicationService documentPublicationService;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PublicationMergeCheck)")
    public Object checkPublicationsMerge(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var method = AspectUtil.getMethod(joinPoint);
        var annotation = method.getAnnotation(PublicationMergeCheck.class);

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        var role = UserRole.valueOf(tokenUtil.extractUserRoleFromToken(tokenValue));
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);
        int personId = userService.getPersonIdForUser(tokenUtil.extractUserIdFromToken(tokenValue));

        List<Integer> documentIds = new ArrayList<>();
        if (attributeMap.containsKey("leftDocumentId") &&
            attributeMap.containsKey("rightDocumentId")) {
            if (!annotation.value().equalsIgnoreCase("MERGE")) {
                documentIds.add(Integer.parseInt(attributeMap.get("leftDocumentId")));
            }
            documentIds.add(Integer.parseInt(attributeMap.get("rightDocumentId")));
        }

        for (var documentId : documentIds) {
            List<Integer> contributors = getContributors(documentId);

            switch (role) {
                case ADMIN:
                    break;
                case RESEARCHER:
                    // TODO: Should we allow this?
                    if (!contributors.contains(personId)) {
                        throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
                    }
                    break;
                case INSTITUTIONAL_EDITOR:
                    if (noResearchersFromUserInstitution(contributors, userId) &&
                        isDocumentNotAThesis(documentId, userId)) {
                        throw new CantEditException(
                            "unauthorizedPublicationEditAttemptByEmployeeMessage");
                    }
                    break;
            }
        }

        return joinPoint.proceed();
    }

    private boolean noResearchersFromUserInstitution(List<Integer> contributors,
                                                     Integer userId) {
        return contributors.stream()
            .filter(contributorId -> contributorId > 0) // filter out external affiliates
            .noneMatch(
                contributorId -> personService.isPersonEmployedInOrganisationUnit(
                    contributorId,
                    userService.getUserOrganisationUnitId(userId)));
    }

    private boolean isDocumentNotAThesis(Integer documentId, Integer userId) {
        var userInstitutionId = userService.getUserOrganisationUnitId(userId);
        var institutionSubUnitIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitutionId);

        var document = documentPublicationService.findOne(documentId);
        return !(document instanceof Thesis) || !institutionSubUnitIds.contains(
            ((Thesis) document).getOrganisationUnit().getId());
    }

    private List<Integer> getContributors(Integer documentId) {
        return documentPublicationService.getContributorIds(documentId);
    }
}
