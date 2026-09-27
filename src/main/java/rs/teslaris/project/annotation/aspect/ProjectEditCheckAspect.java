package rs.teslaris.project.annotation.aspect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.aspect.AspectUtil;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.project.annotation.ProjectEditCheck;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Aspect
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectEditCheckAspect {

    private final ProjectService projectService;

    private final ProjectDocumentRepository projectDocumentRepository;

    private final ProjectEventRepository projectEventRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @Around("@annotation(rs.teslaris.project.annotation.ProjectEditCheck)")
    public Object checkProjectEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        var request = AspectUtil.getRequest();

        var method = AspectUtil.getMethod(joinPoint);
        var annotation = method.getAnnotation(ProjectEditCheck.class);

        var tokenValue = AspectUtil.extractToken(request);

        String role = tokenUtil.extractUserRoleFromToken(tokenValue);
        var userId = tokenUtil.extractUserIdFromToken(tokenValue);
        int personId = userService.getPersonIdForUser(userId);

        List<Integer> contributorPersonIds;
        List<Integer> contributingOrganisationUnitIds;

        if (annotation.value().equalsIgnoreCase("CREATE")) {
            var projectDTO = getProjectFromDTO(joinPoint);

            contributorPersonIds = projectDTO.getPersons().stream()
                .map(PersonProjectContributionDTO::getPersonId)
                .filter(Objects::nonNull)
                .toList();

            contributingOrganisationUnitIds = projectDTO.getOrganisations().stream()
                .map(OrganisationUnitProjectContributionDTO::getOrganisationUnitId)
                .filter(Objects::nonNull)
                .toList();
        } else {
            var projectId = resolveProjectId(joinPoint, AspectUtil.getUriVariables(request));

            contributorPersonIds = projectService.getContributorIds(projectId);
            contributingOrganisationUnitIds =
                projectService.getContributingOrganisationUnitIds(projectId);
        }

        checkPermission(role, personId, userId, contributorPersonIds,
            contributingOrganisationUnitIds);

        return joinPoint.proceed();
    }

    private ProjectDTO getProjectFromDTO(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof ProjectDTO projectDTO) {
                return projectDTO;
            }
        }

        throw new IllegalArgumentException("Missing project data.");
    }

    private Integer resolveProjectId(ProceedingJoinPoint joinPoint,
                                     Map<String, String> attributeMap) {
        if (Objects.nonNull(attributeMap)) {
            if (attributeMap.containsKey("projectId")) {
                return Integer.parseInt(attributeMap.get("projectId"));
            }

            if (attributeMap.containsKey("projectDocumentId")) {
                return projectDocumentRepository
                    .findById(Integer.parseInt(attributeMap.get("projectDocumentId")))
                    .map(projectDocument -> projectDocument.getProject().getId())
                    .orElseThrow(
                        () -> new NotFoundException("Project document relation does not exist."));
            }

            if (attributeMap.containsKey("projectEventId")) {
                return projectEventRepository
                    .findById(Integer.parseInt(attributeMap.get("projectEventId")))
                    .map(projectEvent -> projectEvent.getProject().getId())
                    .orElseThrow(
                        () -> new NotFoundException("Project event relation does not exist."));
            }
        }

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof ProjectDocumentDTO projectDocumentDTO) {
                return projectDocumentDTO.getProjectId();
            }

            if (arg instanceof ProjectEventDTO projectEventDTO) {
                return projectEventDTO.getProjectId();
            }
        }

        throw new IllegalArgumentException("Missing project identifier.");
    }

    private void checkPermission(String role, int personId, int userId,
                                 List<Integer> contributorPersonIds,
                                 List<Integer> contributingOrganisationUnitIds) {
        UserRole userRole = UserRole.valueOf(role);
        switch (userRole) {
            case ADMIN:
                break;
            case RESEARCHER:
                if (!contributorPersonIds.contains(personId)) {
                    throw new CantEditException("unauthorizedProjectEditAttemptMessage");
                }
                break;
            case INSTITUTIONAL_EDITOR:
                if (!isEditorLinkedToProject(contributorPersonIds,
                    contributingOrganisationUnitIds, userId)) {
                    handleUnauthorisedUser();
                }
                break;
            default:
                handleUnauthorisedUser();
        }
    }

    private void handleUnauthorisedUser() {
        throw new CantEditException("unauthorizedProjectEditAttemptByEmployeeMessage");
    }

    private boolean isEditorLinkedToProject(List<Integer> contributorPersonIds,
                                            List<Integer> contributingOrganisationUnitIds,
                                            Integer userId) {
        var editorInstitutionId = userService.getUserOrganisationUnitId(userId);
        var institutionSubUnitIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(editorInstitutionId);

        if (contributingOrganisationUnitIds.stream().anyMatch(institutionSubUnitIds::contains)) {
            return true;
        }

        return contributorPersonIds.stream()
            .filter(contributorId -> contributorId > 0) // filter out external affiliates
            .anyMatch(contributorId -> personService.isPersonEmployedInOrganisationUnit(
                contributorId, editorInstitutionId));
    }
}
