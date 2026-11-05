package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
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
    private IdentifierService identifierService;

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

    private static Identifier identifier(Integer id) {
        var identifier = new Identifier();
        identifier.setId(id);
        return identifier;
    }

    @Test
    public void shouldAttachIdentifierToCreatedProject() {
        var project = project(1);
        var identifier = new Identifier();
        when(projectService.createProject(any())).thenReturn(project);
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier);

        var migrated = writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        var captor = ArgumentCaptor.forClass(ProjectIdentifier.class);
        verify(projectIdentifierRepository).save(captor.capture());
        assertEquals(project, captor.getValue().getProject());
        assertEquals(identifier, captor.getValue().getIdentifier());
        assertEquals("12345ABC", captor.getValue().getValue());
        verify(entityIdentifierRepository, never()).existsByValueForType(any(), any(), any());
        assertTrue(migrated.droppedIdentifiers().isEmpty());
    }

    @Test
    public void shouldNotAttachIdentifierTwiceOnRerun() {
        when(projectService.findOne(1)).thenReturn(project(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier(3));
        when(projectIdentifierRepository.existsByProjectIdAndIdentifierIdAndValue(1, 3,
            "12345ABC")).thenReturn(true);

        var migrated = writer.migrate(new ProjectDTO(), 1, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        assertTrue(migrated.droppedIdentifiers().isEmpty());
        verify(projectService).updateProject(any(), any());
        verify(projectIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldDropIdentifierTakenByAnotherProject() {
        when(projectService.findOne(1)).thenReturn(project(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier(3));
        when(entityIdentifierRepository.existsByValueForType("12345ABC", ProjectIdentifier.class,
            null)).thenReturn(true);

        var migrated = writer.migrate(new ProjectDTO(), 1, Map.of(CODE, "12345ABC"));

        assertEquals("value 12345ABC is already taken by another project",
            migrated.droppedIdentifiers().get(CODE));
        verify(projectIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldDropIdentifierWithoutDefinition() {
        when(projectService.createProject(any())).thenReturn(project(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(null);

        var migrated = writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC"));

        assertEquals("no identifier is configured for code " + CODE,
            migrated.droppedIdentifiers().get(CODE));
        verify(projectIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldNotAttachIdentifierWhenProjectIsNotCreated() {
        when(projectService.createProject(any())).thenReturn(null);

        assertNull(writer.migrate(new ProjectDTO(), null, Map.of(CODE, "12345ABC")));
        verify(projectIdentifierRepository, never()).save(any());
    }
}
