package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.project.migration.ProjectMigrationSupplementerImpl;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectMigrationSupplementerTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private PersonProjectContributionService personProjectContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ResearchAreaService researchAreaService;

    @InjectMocks
    private ProjectMigrationSupplementerImpl supplementer;

    private Project project;


    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(9);
        when(projectRepository.findById(9)).thenReturn(Optional.of(project));
        when(personProjectContributionService.createContribution(any(), eq(project)))
            .thenAnswer(invocation -> {
                PersonProjectContributionDTO dto = invocation.getArgument(0);
                // Saved by the service, so it has an id: entities with a null id collapse in a Set
                var contribution = new PersonProjectContribution();
                contribution.setId(1000 + dto.getOrderNumber());
                contribution.setOrderNumber(dto.getOrderNumber());
                return contribution;
            });
    }

    private static PersonProjectContribution contribution(Integer personId, String name,
                                                          int orderNumber) {
        var contribution = new PersonProjectContribution();
        contribution.setId(orderNumber);
        contribution.setOrderNumber(orderNumber);
        contribution.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        if (personId != null) {
            var person = new Person();
            person.setId(personId);
            contribution.setPerson(person);
        }
        var statement = new AffiliationStatement();
        statement.setDisplayPersonName(new PersonName(null, null, name, null, null, null));
        contribution.setAffiliationStatement(statement);
        return contribution;
    }

    private static PersonProjectContributionDTO member(Integer personId, String name) {
        var dto = new PersonProjectContributionDTO();
        dto.setPersonId(personId);
        dto.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        dto.setInvestigationRole(PersonProjectInvestigationRole.RESEARCHER);
        dto.setDateFrom(LocalDate.of(2019, 1, 1));
        if (name != null) {
            var personName = new PersonNameDTO();
            personName.setLastname(name);
            dto.setPersonName(personName);
        }
        return dto;
    }

    private static ProjectDTO source() {
        var dto = new ProjectDTO();
        dto.setDateFrom(LocalDate.of(2018, 1, 1));
        dto.setDateTo(LocalDate.of(2021, 1, 1));
        dto.setNationalId("PTDC/ABC/1234/2020");
        dto.setInternalIdentifiers(Set.of("PTDC/ABC/1234/2020"));
        return dto;
    }

    @Test
    void shouldFillOnlyEmptyProjectFields() {
        project.setDateFrom(LocalDate.of(2010, 5, 5));
        project.setNationalId("PTDC/XYZ/9999/2010");
        project.setInternalIdentifiers(new HashSet<>(Set.of("OLD-1")));

        supplementer.supplement(9, source(), List.of());

        assertEquals(LocalDate.of(2010, 5, 5), project.getDateFrom());
        assertEquals(LocalDate.of(2021, 1, 1), project.getDateTo());
        assertEquals("PTDC/XYZ/9999/2010", project.getNationalId());
        assertEquals(Set.of("OLD-1", "PTDC/ABC/1234/2020"), project.getInternalIdentifiers());
        verify(projectRepository).save(project);
        verify(projectService).refreshProjectIndex(9);
    }

    @Test
    void shouldFillEmptyStringNationalId() {
        project.setNationalId("");

        supplementer.supplement(9, source(), List.of());

        assertEquals("PTDC/ABC/1234/2020", project.getNationalId());
    }

    @Test
    void shouldNotTakeNationalIdOfAnotherProject() {
        when(projectRepository.existsByNationalId("PTDC/ABC/1234/2020", 9)).thenReturn(true);

        supplementer.supplement(9, source(), List.of());

        assertNull(project.getNationalId());
    }

    @Test
    void shouldFillMissingRoleOfExistingMemberAndAppendNewOnes() {
        var existingOwner = contribution(5, "Test Owner", 1);
        existingOwner.setDateFrom(LocalDate.of(2015, 1, 1));
        var existingByName = contribution(null, "Jane Roe", 2);
        project.setPersons(new HashSet<>(Set.of(existingOwner, existingByName)));

        var dropped = supplementer.supplement(9, source(), List.of(
            member(5, null), member(8, "Test Person"), member(null, " jane roe ")));

        assertEquals(PersonProjectInvestigationRole.RESEARCHER,
            existingOwner.getInvestigationRole());
        assertEquals(LocalDate.of(2015, 1, 1), existingOwner.getDateFrom());
        assertEquals(3, project.getPersons().size());
        assertTrue(dropped.isEmpty());
        verify(personProjectContributionService).createContribution(
            argThat(dto -> dto.getPersonId() == 8 && dto.getOrderNumber() == 3), eq(project));
    }

    @Test
    void shouldReportDifferingTypeAndRoleOfExistingMember() {
        var existing = contribution(5, "Test Owner", 1);
        existing.setInvestigationRole(PersonProjectInvestigationRole.SUPERVISOR);
        project.setPersons(new HashSet<>(Set.of(existing)));
        var member = member(5, null);
        member.setContributionType(PersonProjectContributionType.PRINCIPLE_INVESTIGATOR);

        var dropped = supplementer.supplement(9, source(), List.of(member));

        assertEquals(PersonProjectContributionType.TEAM_MEMBER, existing.getContributionType());
        assertEquals(PersonProjectInvestigationRole.SUPERVISOR, existing.getInvestigationRole());
        assertEquals(List.of(
            "contribution type PRINCIPLE_INVESTIGATOR of contribution 1 not set: it already " +
                "has TEAM_MEMBER",
            "investigation role RESEARCHER of contribution 1 not set: it already has SUPERVISOR"),
            dropped);
    }
}
