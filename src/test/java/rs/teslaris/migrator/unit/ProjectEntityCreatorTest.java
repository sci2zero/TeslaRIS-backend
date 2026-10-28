package rs.teslaris.migrator.unit;

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
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingPartFactory;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.converter.hydrator.ProjectEntityCreator;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.PersonProjectContributionType;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectEntityCreatorTest {

    private static final MigrationRun RUN = MigrationRun.builder().id("run-1").build();

    private static final HydratorCVModel.Institution COORDINATOR =
        new HydratorCVModel.Institution("Test University", null, null, null);

    private static final HydratorCVModel.Institution PARTNER =
        new HydratorCVModel.Institution("Test Partner", null, null, null);

    @Mock
    private ProjectService projectService;

    @Mock
    private HydratorInstitutionResolver institutionResolver;

    @Mock
    private HydratorPersonResolver personResolver;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private MigrationIdResolver idResolver;

    @Mock
    private MigrationLog migrationLog;

    private ProjectEntityCreator creator;

    private static OrganisationUnitProjectContribution contribution(int id, int orderNumber) {
        var contribution = new OrganisationUnitProjectContribution();
        contribution.setId(id);
        contribution.setOrderNumber(orderNumber);
        return contribution;
    }

    @BeforeEach
    void setUp() {
        var conversionUtil = new HydratorConversionUtil(mock(LanguageTagService.class),
            mock(LanguageService.class), mock(ResearchAreaService.class));
        creator = new ProjectEntityCreator(projectService, institutionResolver, personResolver,
            conversionUtil,
            new HydratorFundingPartFactory(currencyService, conversionUtil, migrationLog),
            idResolver, migrationLog);

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
                    OrganisationUnitProjectContributionType.COORDINATOR, COORDINATOR, 1, null),
                new ProjectMigrationDTO.ConsortiumEntry(
                    OrganisationUnitProjectContributionType.PARTNER, PARTNER, 2, null)), List.of(),
                null),
            RUN);

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
            eq("CW-002-MAP-000022"),
            eq("PARTNER not linked, kept as display name: identifier not found"));
    }

    @Test
    void shouldFailItemWithInvalidSourceValue() {
        assertThrows(InvalidSourceValueException.class, () -> creator.create(
            new ProjectMigrationDTO("TEST001", new ProjectDTO(), List.of(), List.of(),
                "invalid date 'x'"),
            RUN));
        verify(projectService, never()).createProject(any());
    }

    @Test
    void shouldAddTeamContributions() {
        var dto = new ProjectDTO();
        when(personResolver.resolve("AAAA-BBBB-CCCC", null))
            .thenReturn(new HydratorPersonResolver.Match(9, null));
        when(personResolver.resolve(null, null))
            .thenReturn(new HydratorPersonResolver.Match(null, "person has no identifier"));

        creator.create(new ProjectMigrationDTO("TEST001", dto, List.of(), List.of(
            new ProjectMigrationDTO.TeamEntry(PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
                "John Doe", "AAAA-BBBB-CCCC", null, 1),
            new ProjectMigrationDTO.TeamEntry(PersonProjectContributionType.TEAM_MEMBER,
                "Jane Smith", null, null, 2),
            new ProjectMigrationDTO.TeamEntry(PersonProjectContributionType.TEAM_MEMBER,
                "Test Member", null, null, 3)), null), RUN);

        var linked = dto.getPersons().get(0);
        assertEquals(9, linked.getPersonId());
        assertEquals("John Doe", linked.getPersonName().getLastname());
        assertNull(linked.getInvestigationRole());
        assertNull(dto.getPersons().get(1).getPersonId());
        assertEquals(3, dto.getPersons().get(2).getOrderNumber());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("CW-002-MAP-000028"), eq("2 team member(s) TEAM_MEMBER not linked, name only: " +
                "person has no identifier"));
    }

    @Test
    void shouldLinkAmountToContributionOnlyWhenFundingIsNotDetermined() {
        var dto = new ProjectDTO();
        var euro = new Currency();
        euro.setId(3);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(euro);
        when(institutionResolver.resolve(PARTNER))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));

        creator.create(new ProjectMigrationDTO("TEST001", dto, List.of(
            new ProjectMigrationDTO.ConsortiumEntry(
                OrganisationUnitProjectContributionType.PARTNER, PARTNER, 1,
                new HydratorProjectModel.Amount("EUR", "1000"))), List.of(), null), RUN);

        var parts = dto.getOrganisations().getFirst().getFundingParts();
        assertEquals(1, parts.size());
        assertNull(parts.getFirst().getFundingId());
        assertEquals(1000, parts.getFirst().getAmount().getAmount());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("CW-002-MAP-000022"),
            eq("1 consortium amount(s) linked to contributions only: funding not determined"));
    }

    @Test
    void shouldRememberConsortiumContributions() {
        var project = new Project();
        project.setId(42);
        project.getOrganisations().add(contribution(101, 1));
        project.getOrganisations().add(contribution(102, 2));
        when(projectService.createProject(any())).thenReturn(project);

        creator.create(new ProjectMigrationDTO("TEST001", new ProjectDTO(), List.of(), List.of(),
            null), RUN);

        verify(idResolver).remember(HydratorSource.NAME,
            MigrationEntityType.PROJECT_ORGANISATION, "TEST001#consortium#1", 101, RUN);
        verify(idResolver).remember(HydratorSource.NAME,
            MigrationEntityType.PROJECT_ORGANISATION, "TEST001#consortium#2", 102, RUN);
    }
}
