package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.ProjectEntityCreator;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectEntityCreatorTest {

    private static final HydratorCVModel.Institution COORDINATOR =
        new HydratorCVModel.Institution("Test University", null, null, null);

    private static final HydratorCVModel.Institution PARTNER =
        new HydratorCVModel.Institution("Test Partner", null, null, null);

    @Mock
    private ProjectService projectService;

    @Mock
    private HydratorInstitutionResolver institutionResolver;

    @Mock
    private MigrationLog migrationLog;

    private ProjectEntityCreator creator;


    @BeforeEach
    void setUp() {
        creator = new ProjectEntityCreator(projectService, institutionResolver,
            new HydratorConversionUtil(mock(LanguageTagService.class), mock(LanguageService.class),
                mock(ResearchAreaService.class)),
            migrationLog);

        var project = new Project();
        project.setId(42);
        when(projectService.createProject(any())).thenReturn(project);
    }

    @Test
    void shouldAddConsortiumContributions() {
        var dto = new ProjectDTO();
        when(institutionResolver.resolve(COORDINATOR))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));
        when(institutionResolver.resolve(PARTNER))
            .thenReturn(new HydratorInstitutionResolver.Match(null, "identifier not found"));

        var id = creator.create(new ProjectMigrationDTO("TEST001", dto, List.of(
            new ProjectMigrationDTO.ConsortiumEntry(
                OrganisationUnitProjectContributionType.COORDINATOR, COORDINATOR, 1),
            new ProjectMigrationDTO.ConsortiumEntry(
                OrganisationUnitProjectContributionType.PARTNER, PARTNER, 2)), null), true);

        assertEquals(42, id);
        var linked = dto.getOrganisations().get(0);
        assertEquals(7, linked.getOrganisationUnitId());
        assertEquals(OrganisationUnitProjectContributionType.COORDINATOR,
            linked.getContributionType());
        assertEquals(1, linked.getOrderNumber());

        var display = dto.getOrganisations().get(1);
        assertNull(display.getOrganisationUnitId());
        assertEquals("Test Partner", display.getDisplayOrganisationUnit().getFirst().getContent());
        assertEquals(2, display.getOrderNumber());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("MAP-022"), eq("PARTNER not linked, kept as display name: identifier not found"));
    }

    @Test
    void shouldFailItemWithInvalidSourceValue() {
        assertThrows(InvalidSourceValueException.class, () -> creator.create(
            new ProjectMigrationDTO("TEST001", new ProjectDTO(), List.of(), "invalid date 'x'"),
            true));
        verify(projectService, never()).createProject(any());
    }
}
