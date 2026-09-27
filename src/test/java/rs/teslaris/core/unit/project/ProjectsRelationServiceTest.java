package rs.teslaris.core.unit.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectsRelation;
import rs.teslaris.project.model.project.ProjectsRelationType;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.repository.project.ProjectsRelationRepository;
import rs.teslaris.project.service.impl.project.ProjectsRelationServiceImpl;

@SpringBootTest
public class ProjectsRelationServiceTest {

    @Mock
    private ProjectsRelationRepository projectsRelationRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectsRelationServiceImpl projectsRelationService;

    private static ProjectsRelationDTO relationDTO(Integer targetProjectId) {
        var dto = new ProjectsRelationDTO();
        dto.setRelationType(ProjectsRelationType.PART_OF);
        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2026, 3, 1));
        dto.setTargetProjectId(targetProjectId);
        dto.setSourceProjectDescription(List.of());
        dto.setTargetProjectDescription(List.of());
        return dto;
    }

    @Test
    public void shouldCreateRelationWithTargetProject() {
        // given
        var sourceProject = new Project();
        sourceProject.setId(1);

        var targetProject = new Project();
        targetProject.setId(2);

        var dto = relationDTO(2);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(projectRepository.findById(2)).thenReturn(Optional.of(targetProject));
        when(projectsRelationRepository.save(any(ProjectsRelation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = projectsRelationService.createRelation(dto, sourceProject);

        // then
        assertNotNull(result);
        assertEquals(sourceProject, result.getSourceProject());
        assertEquals(targetProject, result.getTargetProject());
        assertEquals(ProjectsRelationType.PART_OF, result.getRelationType());
        assertEquals(LocalDate.of(2025, 1, 1), result.getDateFrom());
        assertEquals(LocalDate.of(2026, 3, 1), result.getDateTo());
        verify(projectRepository).findById(2);
        verify(projectsRelationRepository).save(any(ProjectsRelation.class));
    }

    @Test
    public void shouldNotSetTargetProjectWhenTargetIdIsNull() {
        // given
        var sourceProject = new Project();
        sourceProject.setId(1);

        var dto = relationDTO(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(projectsRelationRepository.save(any(ProjectsRelation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = projectsRelationService.createRelation(dto, sourceProject);

        // then
        assertNotNull(result);
        assertEquals(sourceProject, result.getSourceProject());
        assertNull(result.getTargetProject());
        verify(projectRepository, never()).findById(anyInt());
    }

    @Test
    public void shouldThrowWhenTargetProjectDoesNotExist() {
        // given
        var sourceProject = new Project();
        sourceProject.setId(1);

        var dto = relationDTO(999);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(projectRepository.findById(999)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class,
            () -> projectsRelationService.createRelation(dto, sourceProject));

        verify(projectsRelationRepository, never()).save(any());
    }

    @Test
    public void shouldNotSetSourceProjectAsTargetProject() {
        // given
        var sourceProject = new Project();
        sourceProject.setId(1);

        var targetProject = new Project();
        targetProject.setId(2);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(projectRepository.findById(2)).thenReturn(Optional.of(targetProject));
        when(projectsRelationRepository.save(any(ProjectsRelation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = projectsRelationService.createRelation(relationDTO(2), sourceProject);

        // then
        assertEquals(2, result.getTargetProject().getId());
    }
}
