package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectCollaborationType;
import rs.teslaris.project.model.project.ProjectResearchType;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.impl.project.ProjectServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @InjectMocks
    private ProjectServiceImpl projectService;

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

}
