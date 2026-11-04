package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.project.migration.ProjectMigrationWriterImpl;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectMigrationWriterTest {

    private static final String CODE = "TESTCODE";

    @Mock
    private ProjectService projectService;

    @Mock
    private IdentifierRepository identifierRepository;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private ProjectIdentifierRepository projectIdentifierRepository;

    @InjectMocks
    private ProjectMigrationWriterImpl writer;


    private static Project project(Integer id) {
        var project = new Project();
        project.setId(id);
        return project;
    }

    @Test
    public void shouldAttachIdentifierToCreatedProject() {
        var project = project(1);
        var identifier = new Identifier();
        when(projectService.createProject(any())).thenReturn(project);
        when(identifierRepository.findByCode(CODE)).thenReturn(identifier);

        var migrated = writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        var captor = ArgumentCaptor.forClass(ProjectIdentifier.class);
        verify(projectIdentifierRepository).save(captor.capture());
        assertEquals(project, captor.getValue().getProject());
        assertEquals(identifier, captor.getValue().getIdentifier());
        assertEquals("12345ABC", captor.getValue().getValue());
    }

    @Test
    public void shouldNotAttachIdentifierTwiceOnRerun() {
        when(projectService.findOne(1)).thenReturn(project(1));
        when(identifierRepository.findByCode(CODE)).thenReturn(new Identifier());
        when(entityIdentifierRepository.existsByValueForType("12345ABC", ProjectIdentifier.class,
            null)).thenReturn(true);

        var migrated = writer.migrate(new ProjectDTO(), 1, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        verify(projectService).updateProject(any(), any());
        verify(projectIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldSkipIdentifierWithoutDefinition() {
        when(projectService.createProject(any())).thenReturn(project(1));
        when(identifierRepository.findByCode(CODE)).thenReturn(null);

        writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC"));

        verify(projectIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldNotAttachIdentifierWhenProjectIsNotCreated() {
        when(projectService.createProject(any())).thenReturn(null);

        assertNull(writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC")));
        verify(projectIdentifierRepository, never()).save(any());
    }
}
