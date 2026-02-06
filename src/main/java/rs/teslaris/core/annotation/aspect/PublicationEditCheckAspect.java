package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.jwt.JwtUtil;

@Aspect
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicationEditCheckAspect {

    private final DocumentPublicationService documentPublicationService;

    private final DocumentLookupService documentLookupService;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.core.annotation.PublicationEditCheck)")
    public Object checkPublicationEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var method = AspectUtil.getMethod(joinPoint);
        var annotation = method.getAnnotation(PublicationEditCheck.class);

        var tokenValue = AspectUtil.extractToken(request);
        var attributeMap = AspectUtil.getUriVariables(request);

        String role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);
        int personId = userService.getPersonIdForUser(tokenUtil.extractUserIdFromToken(tokenValue));

        List<Integer> documentIds = new ArrayList<>();
        if (attributeMap.containsKey("documentId")) {
            documentIds.add(Integer.parseInt(attributeMap.get("documentId")));
        } else if (attributeMap.containsKey("staleThesisId") &&
            attributeMap.containsKey("substituteThesisId") &&
            annotation.value().equalsIgnoreCase("THESIS")) {
            documentIds.add(Integer.parseInt(attributeMap.get("staleThesisId")));
            documentIds.add(Integer.parseInt(attributeMap.get("substituteThesisId")));
        } else if (annotation.value().equalsIgnoreCase("CREATE")) {
            documentIds.add(-1); // not checked
        } else {
            throw new IllegalArgumentException(
                "Missing document identifiers."); // should never happen in prod, only for testing
        }

        var assessmentMode = annotation.value().equalsIgnoreCase("ASSESS");

        for (var documentId : documentIds) {
            List<Integer> contributors =
                getContributors(annotation, attributeMap, joinPoint, documentId);

            checkPermission(role, personId, userId, contributors, joinPoint, annotation,
                assessmentMode, documentId);
        }

        return joinPoint.proceed();
    }

    private List<Integer> getContributors(PublicationEditCheck annotation,
                                          Map<String, String> attributeMap,
                                          ProceedingJoinPoint joinPoint,
                                          Integer documentId) {
        if (annotation.value().equalsIgnoreCase("CREATE")) {
            return getContributorsFromDTO(joinPoint);
        } else {
            return getContributorsFromDatabase(attributeMap, documentId);
        }
    }

    private List<Integer> getContributorsFromDTO(ProceedingJoinPoint joinPoint) {
        var publicationDTO = (DocumentDTO) joinPoint.getArgs()[0];
        return publicationDTO.getContributions().stream().map(PersonContributionDTO::getPersonId)
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isDocumentNotAThesis(ProceedingJoinPoint joinPoint,
                                         PublicationEditCheck annotation,
                                         Integer userId, Integer documentId) {
        var userInstitutionId = userService.getUserOrganisationUnitId(userId);
        var institutionSubUnitIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitutionId);

        if (annotation.value().equalsIgnoreCase("CREATE")) {
            var publicationDTO = (DocumentDTO) joinPoint.getArgs()[0];
            return !(publicationDTO instanceof ThesisDTO) || !institutionSubUnitIds.contains(
                ((ThesisDTO) publicationDTO).getOrganisationUnitId());
        }

        var document =
            documentLookupService.fastDocumentLookup(documentId);
        return !(document instanceof Thesis) || !institutionSubUnitIds.contains(
            ((Thesis) document).getOrganisationUnit().getId());
    }

    private List<Integer> getContributorsFromDatabase(Map<String, String> attributeMap,
                                                      Integer documentId) {
        return documentPublicationService.getContributorIds(documentId);
    }

    private void checkPermission(String role, int personId, int userId,
                                 List<Integer> contributors,
                                 ProceedingJoinPoint joinPoint,
                                 PublicationEditCheck annotation,
                                 boolean assessmentMode,
                                 Integer documentId) {
        UserRole userRole = UserRole.valueOf(role);
        switch (userRole) {
            case ADMIN:
                break;
            case RESEARCHER:
                if (!contributors.contains(personId) &&
                    annotation.value().equalsIgnoreCase("CREATE") &&
                    !((joinPoint.getArgs()[0] instanceof ProceedingsDTO) ||
                        (joinPoint.getArgs()[0] instanceof MonographDTO))) {
                    throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
                }

                if (!contributors.contains(personId) &&
                    annotation.value().equalsIgnoreCase("EDIT")) {
                    throw new CantEditException("unauthorizedPublicationEditAttemptMessage");
                }

                break;
            case INSTITUTIONAL_EDITOR:
                if (assessmentMode || noResearchersFromUserInstitution(contributors, userId) &&
                    isDocumentNotAThesis(joinPoint, annotation, userId, documentId)) {
                    handleUnauthorisedUser();
                }
                break;
            case COMMISSION:
                if (!assessmentMode || noResearchersFromUserInstitution(contributors, userId) &&
                    isDocumentNotAThesis(joinPoint, annotation, userId, documentId)) {
                    handleUnauthorisedUser();
                }
                break;
            case INSTITUTIONAL_LIBRARIAN, HEAD_OF_LIBRARY:
                if (assessmentMode ||
                    isDocumentNotAThesis(joinPoint, annotation, userId, documentId)) {
                    handleUnauthorisedUser();
                }
                break;
            default:
                handleUnauthorisedUser();
        }
    }

    private void handleUnauthorisedUser() {
        throw new CantEditException("unauthorizedPublicationEditAttemptByEmployeeMessage");
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
}
