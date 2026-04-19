package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectCollaborationType;
import rs.teslaris.project.model.project.ProjectResearchType;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.impl.project.ProjectServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

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

}
