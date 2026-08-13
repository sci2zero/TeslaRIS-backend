package rs.teslaris.core.unit.project;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.project.*;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.impl.project.ProjectServiceImpl;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectsRelationService;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ProjectIndexRepository projectIndexRepository;

    @Mock
    private SearchService<ProjectIndex> searchService;

    @Mock
    private PersonProjectContributionService personProjectContributionService;

    @Mock
    private OrganisationUnitProjectContributionService organisationUnitProjectContributionService;

    @Mock
    private ProjectsRelationService projectsRelationService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static ProjectDTO baseProjectDTO() {
        var dto = new ProjectDTO();
        dto.setName(List.of());
        dto.setDescription(List.of());
        dto.setNameAbbreviation(List.of());
        dto.setKeywords(List.of());
        dto.setResearchAreasId(Set.of());
        dto.setStatus(ProjectStatus.ONGOING);
        dto.setCollaborationType(ProjectCollaborationType.NATIONAL);
        dto.setResearchType(ProjectResearchType.INNOVATION);
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now().plusYears(1));
        return dto;
    }

    private static ProjectsRelationDTO relationDTO(Integer targetProjectId) {
        var dto = new ProjectsRelationDTO();
        dto.setRelationType(ProjectsRelationType.PART_OF);
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now().plusYears(1));
        dto.setTargetProjectId(targetProjectId);
        return dto;
    }

    @Test
    public void shouldReturnEmptyPageWhenNoProjectsFound() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var onlyActive = false;
        var allowedStatuses = new ArrayList<ProjectStatus>();
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(ProjectIndex.class), eq("project")))
            .thenReturn(Page.empty());

        // when
        var result = projectService.searchProjects(tokens, dateFrom, dateTo, onlyActive, allowedStatuses ,pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(ProjectIndex.class), eq("project"));
    }

    @Test
    public void shouldReturnProjectsPageWhenResultsExist() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var onlyActive = false;
        var allowedStatuses = new ArrayList<ProjectStatus>();
        var pageable = PageRequest.of(0, 10);

        var projectIndex = new ProjectIndex();
        projectIndex.setDatabaseId(1);
        projectIndex.setNameSr("Test Project");
        projectIndex.setNameOther("Test Project");

        var expectedPage = new PageImpl<>(
            List.of(projectIndex), pageable, 1);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(ProjectIndex.class), eq("project")))
            .thenReturn(expectedPage);

        // when
        var result = projectService.searchProjects(tokens, dateFrom, dateTo, onlyActive, allowedStatuses ,pageable);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().getFirst().getDatabaseId());
        assertEquals("Test Project", result.getContent().getFirst().getNameSr());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(ProjectIndex.class), eq("project"));
    }

    @Test
    public void shouldReturnProjectDTOWhenProjectExists() {
        // given
        var projectId = 1;
        var project = new Project();
        project.setId(projectId);
        project.setStatus(ProjectStatus.ONGOING);
        project.setCollaborationType(ProjectCollaborationType.INTERNAL);
        project.setResearchType(ProjectResearchType.INNOVATION);

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(project));

        // when
        var result = projectService.readProject(projectId);

        // then
        assertNotNull(result);
        assertEquals(projectId, result.getId());
        verify(projectRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenProjectNotFound() {
        // given
        var projectId = 999;

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            projectService.readProject(projectId));
        verify(projectRepository).findById(projectId);
    }

    @Test
    public void shouldCreateProjectSuccessfully() {
        // given
        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of(1, 2));
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateFrom(LocalDate.now());
        projectDTO.setDateTo(LocalDate.now().plusYears(1));
        projectDTO.setUris(Set.of("https://example.com"));

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(500000.0);
        projectDTO.setCosts(monetaryAmountDTO);

        var savedProject = new Project();
        savedProject.setId(1);
        savedProject.setStatus(ProjectStatus.ONGOING);
        savedProject.setCollaborationType(ProjectCollaborationType.NATIONAL);
        savedProject.setResearchType(ProjectResearchType.INNOVATION);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(currencyService.findOne(1))
            .thenReturn(null);
        when(projectRepository.save(any(Project.class)))
            .thenReturn(savedProject);
        when(projectIndexRepository.save(any(ProjectIndex.class)))
            .thenReturn(new ProjectIndex());

        // when
        var result = projectService.createProject(projectDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(multilingualContentService, times(4)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(currencyService).findOne(1);
        verify(projectRepository).save(any(Project.class));
        verify(projectIndexRepository).save(any(ProjectIndex.class));
    }

    @Test
    public void shouldCreateProjectWithoutCosts() {
        // given
        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of());
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateFrom(LocalDate.now());
        projectDTO.setDateTo(LocalDate.now().plusYears(1));
        projectDTO.setCosts(null);

        var savedProject = new Project();
        savedProject.setId(1);
        savedProject.setStatus(ProjectStatus.ONGOING);
        savedProject.setCollaborationType(ProjectCollaborationType.NATIONAL);
        savedProject.setResearchType(ProjectResearchType.INNOVATION);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(projectRepository.save(any(Project.class)))
            .thenReturn(savedProject);
        when(projectIndexRepository.save(any(ProjectIndex.class)))
            .thenReturn(new ProjectIndex());

        // when
        var result = projectService.createProject(projectDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(currencyService, never()).findOne(anyInt());
        verify(projectRepository).save(any(Project.class));
        verify(projectIndexRepository).save(any(ProjectIndex.class));
    }

    @Test
    public void shouldCreateProjectWithTeamMembers() {
        // given
        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of());
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateFrom(LocalDate.now());
        projectDTO.setDateTo(LocalDate.now().plusYears(1));

        var member1 = new PersonProjectContributionDTO();
        var member2 = new PersonProjectContributionDTO();
        projectDTO.setPersons(List.of(member1, member2));

        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionService.createContribution(any(), any()))
                .thenReturn(new PersonProjectContribution());
        when(projectRepository.save(any(Project.class)))
                .thenReturn(new Project());
        when(projectIndexRepository.save(any(ProjectIndex.class)))
                .thenReturn(new ProjectIndex());

        // when
        projectService.createProject(projectDTO);

        // then
        verify(personProjectContributionService, times(2))
                .createContribution(any(), any());
    }

    @Test
    public void shouldCreateProjectWithRelations() {
        // given
        var projectDTO = baseProjectDTO();
        projectDTO.setRelations(List.of(relationDTO(2), relationDTO(3)));

        var savedProject = new Project();
        savedProject.setId(1);

        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(projectsRelationService.createRelation(any(), any()))
                .thenReturn(new ProjectsRelation());
        when(projectRepository.save(any(Project.class)))
                .thenReturn(savedProject);
        when(projectIndexRepository.save(any(ProjectIndex.class)))
                .thenReturn(new ProjectIndex());

        // when
        var result = projectService.createProject(projectDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(projectsRelationService, times(2)).createRelation(any(), eq(savedProject));
        verify(projectRepository, never()).findById(2);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    public void shouldCreateProjectWithAllCollections() {
        // given
        var projectDTO = baseProjectDTO();
        projectDTO.setPersons(List.of(new PersonProjectContributionDTO(),
                new PersonProjectContributionDTO()));
        projectDTO.setOrganisations(List.of(new OrganisationUnitProjectContributionDTO()));
        projectDTO.setRelations(List.of(relationDTO(2), relationDTO(3)));

        var savedProject = new Project();
        savedProject.setId(1);

        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionService.createContribution(any(), any()))
                .thenReturn(new PersonProjectContribution());
        when(organisationUnitProjectContributionService.createContribution(any(), any()))
                .thenReturn(new OrganisationUnitProjectContribution());
        when(projectsRelationService.createRelation(any(), any()))
                .thenReturn(new ProjectsRelation());
        when(projectRepository.save(any(Project.class)))
                .thenReturn(savedProject);
        when(projectIndexRepository.save(any(ProjectIndex.class)))
                .thenReturn(new ProjectIndex());

        // when
        var result = projectService.createProject(projectDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(personProjectContributionService, times(2))
                .createContribution(any(), eq(savedProject));
        verify(organisationUnitProjectContributionService)
                .createContribution(any(), eq(savedProject));
        verify(projectsRelationService, times(2)).createRelation(any(), eq(savedProject));
        verify(projectIndexRepository).save(any(ProjectIndex.class));
    }

    @Test
    public void shouldThrowWhenCreateAndDatesAreInvalid() {
        // given
        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of());
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateTo(LocalDate.now());
        projectDTO.setDateFrom(LocalDate.now().plusYears(1));

        // when & then
        assertThrows(DateRangeException.class,
            () -> projectService.createProject(projectDTO));

        verify(projectRepository, never()).save(any());
    }

    @Test
    public void shouldUpdateProjectSuccessfully() {
        // given
        var projectId = 1;
        var existingProject = new Project();
        existingProject.setId(projectId);
        existingProject.setName(new HashSet<>());
        existingProject.setDescription(new HashSet<>());
        existingProject.setNameAbbreviation(new HashSet<>());
        existingProject.setKeywords(new HashSet<>());
        existingProject.setResearchAreas(new HashSet<>());
        existingProject.setStatus(ProjectStatus.ONGOING);
        existingProject.setCollaborationType(ProjectCollaborationType.NATIONAL);
        existingProject.setResearchType(ProjectResearchType.INNOVATION);

        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of(1, 2));
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateFrom(LocalDate.now());
        projectDTO.setDateTo(LocalDate.now().plusYears(1));

        var projectIndex = new ProjectIndex();
        projectIndex.setDatabaseId(projectId);

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(existingProject));
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(projectIndexRepository.findProjectIndexByDatabaseId(projectId))
            .thenReturn(Optional.of(projectIndex));

        // when
        projectService.updateProject(projectId, projectDTO);

        // then
        verify(projectRepository).findById(projectId);
        verify(multilingualContentService, times(4)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(projectIndexRepository).findProjectIndexByDatabaseId(projectId);
        verify(projectIndexRepository).save(any(ProjectIndex.class));
    }

    @Test
    public void shouldUpdateProjectSuccessfullyWithCosts() {
        // given
        var projectId = 1;
        var existingProject = new Project();
        existingProject.setId(projectId);
        existingProject.setName(new HashSet<>());
        existingProject.setDescription(new HashSet<>());
        existingProject.setNameAbbreviation(new HashSet<>());
        existingProject.setKeywords(new HashSet<>());
        existingProject.setResearchAreas(new HashSet<>());
        existingProject.setStatus(ProjectStatus.ONGOING);
        existingProject.setCollaborationType(ProjectCollaborationType.NATIONAL);
        existingProject.setResearchType(ProjectResearchType.INNOVATION);

        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of());
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateFrom(LocalDate.now());
        projectDTO.setDateTo(LocalDate.now().plusYears(1));

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(500000.0);
        projectDTO.setCosts(monetaryAmountDTO);

        var projectIndex = new ProjectIndex();
        projectIndex.setDatabaseId(projectId);

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(existingProject));
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(currencyService.findOne(1))
            .thenReturn(null);
        when(projectIndexRepository.findProjectIndexByDatabaseId(projectId))
            .thenReturn(Optional.of(projectIndex));

        // when
        projectService.updateProject(projectId, projectDTO);

        // then
        verify(projectRepository).findById(projectId);
        verify(multilingualContentService, times(4)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(currencyService).findOne(1);
        verify(projectIndexRepository).findProjectIndexByDatabaseId(projectId);
        verify(projectIndexRepository).save(any(ProjectIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentProject() {
        // given
        var projectId = 999;
        var projectDTO = new ProjectDTO();

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            projectService.updateProject(projectId, projectDTO));
        verify(projectRepository).findById(projectId);
        verify(projectIndexRepository, never()).findProjectIndexByDatabaseId(anyInt());
    }

    @Test
    public void shouldThrowWhenUpdateAndDatesAreInvalid() {
        // given
        var projectId = 1;
        var existingProject = new Project();
        existingProject.setId(projectId);
        existingProject.setName(new HashSet<>());
        existingProject.setDescription(new HashSet<>());
        existingProject.setNameAbbreviation(new HashSet<>());
        existingProject.setKeywords(new HashSet<>());
        existingProject.setResearchAreas(new HashSet<>());
        existingProject.setStatus(ProjectStatus.ONGOING);
        existingProject.setCollaborationType(ProjectCollaborationType.NATIONAL);
        existingProject.setResearchType(ProjectResearchType.INNOVATION);

        var projectDTO = new ProjectDTO();
        projectDTO.setName(List.of());
        projectDTO.setDescription(List.of());
        projectDTO.setNameAbbreviation(List.of());
        projectDTO.setKeywords(List.of());
        projectDTO.setResearchAreasId(Set.of());
        projectDTO.setStatus(ProjectStatus.ONGOING);
        projectDTO.setCollaborationType(ProjectCollaborationType.NATIONAL);
        projectDTO.setResearchType(ProjectResearchType.INNOVATION);
        projectDTO.setDateTo(LocalDate.now());
        projectDTO.setDateFrom(LocalDate.now().plusYears(1));

        when(projectRepository.findById(projectId))
            .thenReturn(Optional.of(existingProject));

        // when & then
        assertThrows(DateRangeException.class,
            () -> projectService.updateProject(projectId, projectDTO));

        verify(projectRepository).findById(projectId);
        verify(projectIndexRepository, never()).findProjectIndexByDatabaseId(anyInt());
    }

    @Test
    public void shouldAddPersonToProject() {
        // given
        var projectId = 1;
        var project = new Project();
        project.setId(projectId);

        var contribution = new PersonProjectContribution();
        contribution.setId(10);
        contribution.setOrderNumber(1);
        contribution.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        contribution.setInvestigationRole(PersonProjectInvestigationRole.RESEARCHER);

        var dto = new PersonProjectContributionDTO();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(personProjectContributionService.createContribution(dto, project))
                .thenReturn(contribution);

        // when
        var result = projectService.addPerson(projectId, dto);

        // then
        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(PersonProjectContributionType.TEAM_MEMBER, result.getContributionType());
        assertEquals(PersonProjectInvestigationRole.RESEARCHER, result.getInvestigationRole());
        assertTrue(project.getPersons().contains(contribution));
        verify(projectRepository).save(project);
    }

    @Test
    public void shouldRemovePersonFromProject() {
        // given
        var projectId = 1;
        var contributionId = 10;

        var contribution = new PersonProjectContribution();
        contribution.setId(contributionId);

        var project = new Project();
        project.setId(projectId);
        project.getPersons().add(contribution);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(personProjectContributionService.findOne(contributionId)).thenReturn(contribution);

        // when
        projectService.removePerson(projectId, contributionId);

        // then
        assertTrue(project.getPersons().isEmpty());
        verify(projectRepository).save(project);
    }

    @Test
    public void shouldAddOrganisationToProject() {
        // given
        var projectId = 1;
        var project = new Project();
        project.setId(projectId);

        var contribution = new OrganisationUnitProjectContribution();
        contribution.setId(20);
        contribution.setOrderNumber(1);
        contribution.setContributionType(OrganisationUnitProjectContributionType.PARTNER);

        var dto = new OrganisationUnitProjectContributionDTO();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(organisationUnitProjectContributionService.createContribution(dto, project))
                .thenReturn(contribution);

        // when
        var result = projectService.addOrganisation(projectId, dto);

        // then
        assertNotNull(result);
        assertEquals(20, result.getId());
        assertEquals(OrganisationUnitProjectContributionType.PARTNER,
                result.getContributionType());
        assertEquals(1, result.getOrderNumber());
        assertTrue(project.getOrganisations().contains(contribution));
        verify(projectRepository).save(project);
    }

    @Test
    public void shouldRemoveOrganisationFromProject() {
        // given
        var projectId = 1;
        var contributionId = 20;

        var contribution = new OrganisationUnitProjectContribution();
        contribution.setId(contributionId);

        var project = new Project();
        project.setId(projectId);
        project.getOrganisations().add(contribution);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(organisationUnitProjectContributionService.findOne(contributionId))
                .thenReturn(contribution);

        // when
        projectService.removeOrganisation(projectId, contributionId);

        // then
        assertTrue(project.getOrganisations().isEmpty());
        verify(projectRepository).save(project);
    }

    @Test
    public void shouldAddRelationToProject() {
        // given
        var projectId = 1;
        var project = new Project();
        project.setId(projectId);

        var targetProject = new Project();
        targetProject.setId(2);

        var relation = new ProjectsRelation();
        relation.setId(30);
        relation.setRelationType(ProjectsRelationType.PREDECESSOR);
        relation.setSourceProject(project);
        relation.setTargetProject(targetProject);

        var dto = relationDTO(2);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectsRelationService.createRelation(dto, project)).thenReturn(relation);

        // when
        var result = projectService.addProjectRelation(projectId, dto);

        // then
        assertNotNull(result);
        assertEquals(30, result.getId());
        assertEquals(1, result.getSourceProjectId());
        assertEquals(2, result.getTargetProjectId());
        assertEquals(ProjectsRelationType.PREDECESSOR, result.getRelationType());
        assertTrue(project.getRelatedProjects().contains(relation));
        verify(projectRepository).save(project);
    }

    @Test
    public void shouldRemoveRelationFromProject() {
        // given
        var projectId = 1;
        var relationId = 30;

        var relation = new ProjectsRelation();
        relation.setId(relationId);

        var project = new Project();
        project.setId(projectId);
        project.getRelatedProjects().add(relation);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectsRelationService.findOne(relationId)).thenReturn(relation);

        // when
        projectService.removeProjectRelation(projectId, relationId);

        // then
        assertTrue(project.getRelatedProjects().isEmpty());
        verify(projectRepository).save(project);
    }
}
