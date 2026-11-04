package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.dto.identifier.ProjectIdentifierDTO;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.service.impl.identifier.ProjectIdentifierServiceImpl;
import rs.teslaris.project.service.impl.identifier.cruddelegate.ProjectIdentifierJPAServiceImpl;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@SpringBootTest
public class ProjectIdentifierServiceTest {

    @Mock
    private ProjectIdentifierRepository projectIdentifierRepository;

    @Mock
    private ProjectIdentifierJPAServiceImpl projectIdentifierJPAService;

    @Mock
    private ProjectService projectService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private ProjectIdentifierServiceImpl projectIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllProjectIdentifiersForProject(AccessLevel accessLevel) {
        // Given
        var projectId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.PROJECT)));

        var projectIdentifier1 = new ProjectIdentifier();
        projectIdentifier1.setIdentifier(identifier);

        var projectIdentifier2 = new ProjectIdentifier();
        projectIdentifier2.setIdentifier(identifier);

        when(projectIdentifierRepository.findIdentifiersForProjectAndIdentifierAccessLevel(projectId,
            accessLevel)).thenReturn(List.of(projectIdentifier1, projectIdentifier2));

        // When
        var response = projectIdentifierService.getIdentifiersForProject(projectId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(projectIdentifierRepository)
            .findIdentifiersForProjectAndIdentifierAccessLevel(projectId, accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForProject() {
        // Given
        var projectId = 1;

        when(projectIdentifierRepository.findIdentifiersForProjectAndIdentifierAccessLevel(projectId,
            AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response = projectIdentifierService.getIdentifiersForProject(projectId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateProjectIdentifier() {
        // Given
        var projectIdentifierDTO = new ProjectIdentifierDTO();
        projectIdentifierDTO.setProjectId(1);
        projectIdentifierDTO.setIdentifierId(1);
        projectIdentifierDTO.setValue("10.1234/test");

        var newProjectIdentifier = new ProjectIdentifier();
        newProjectIdentifier.setIdentifier(new Identifier());

        when(projectService.findOne(1)).thenReturn(new Project());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(projectIdentifierJPAService.save(any(ProjectIdentifier.class)))
            .thenReturn(newProjectIdentifier);

        // When
        var result = projectIdentifierService.createProjectIdentifier(projectIdentifierDTO);

        // Then
        assertNotNull(result);
        verify(projectService).findOne(1);
        verify(projectIdentifierJPAService).save(any(ProjectIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingProjectIdentifierWithNonExistentProject() {
        // Given
        var projectIdentifierDTO = new ProjectIdentifierDTO();
        projectIdentifierDTO.setProjectId(99);
        projectIdentifierDTO.setIdentifierId(1);
        projectIdentifierDTO.setValue("10.1234/test");

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(projectService.findOne(99)).thenThrow(new NotFoundException("Project not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            projectIdentifierService.createProjectIdentifier(projectIdentifierDTO));

        verify(projectIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdateProjectIdentifier() {
        // Given
        var projectIdentifierId = 1;
        var projectIdentifierDTO = new ProjectIdentifierDTO();
        projectIdentifierDTO.setProjectId(1);
        projectIdentifierDTO.setIdentifierId(1);
        projectIdentifierDTO.setValue("10.1234/updated");

        var existingProjectIdentifier = new ProjectIdentifier();
        existingProjectIdentifier.setIdentifier(new Identifier());

        when(projectIdentifierJPAService.findOne(projectIdentifierId))
            .thenReturn(existingProjectIdentifier);
        when(projectService.findOne(1)).thenReturn(new Project());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        projectIdentifierService.updateProjectIdentifier(projectIdentifierId, projectIdentifierDTO);

        // Then
        verify(projectIdentifierJPAService).findOne(projectIdentifierId);
        verify(projectService).findOne(1);
        verify(projectIdentifierJPAService).save(existingProjectIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProjectIdentifier() {
        // Given
        var projectIdentifierId = 99;
        var projectIdentifierDTO = new ProjectIdentifierDTO();
        projectIdentifierDTO.setProjectId(1);
        projectIdentifierDTO.setIdentifierId(1);

        when(projectIdentifierJPAService.findOne(projectIdentifierId))
            .thenThrow(new NotFoundException("Project identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            projectIdentifierService.updateProjectIdentifier(projectIdentifierId, projectIdentifierDTO));

        verify(projectIdentifierJPAService, never()).save(any());
    }
}
