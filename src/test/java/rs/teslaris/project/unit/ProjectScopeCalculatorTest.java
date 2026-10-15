package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.util.ProjectScopeCalculator;

@SpringBootTest
public class ProjectScopeCalculatorTest {

    @Mock
    private InvolvementRepository involvementRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private ProjectScopeCalculator projectScopeCalculator;


    private OrganisationUnit organisationUnit(Integer id) {
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(id);
        return organisationUnit;
    }

    private PersonProjectContribution personContribution(Integer personId) {
        var contribution = new PersonProjectContribution();

        if (personId != null) {
            var person = new Person();
            person.setId(personId);
            contribution.setPerson(person);
        }

        return contribution;
    }

    private OrganisationUnitProjectContribution organisationContribution(Integer unitId) {
        var contribution = new OrganisationUnitProjectContribution();

        if (unitId != null) {
            contribution.setOrganisationUnit(organisationUnit(unitId));
        }

        return contribution;
    }

    private Funding funding(Integer funderId) {
        var funding = new Funding();

        if (funderId != null) {
            funding.setFunder(organisationUnit(funderId));
        }

        return funding;
    }

    @Test
    public void shouldAnswerWithSystemContributorsOnly() {
        // given
        var project = new Project();
        project.getPersons().addAll(List.of(personContribution(11), personContribution(null)));
        project.getOrganisations()
            .addAll(List.of(organisationContribution(22), organisationContribution(null)));

        when(organisationUnitService.getSuperOUsHierarchyRecursive(anyInt()))
            .thenReturn(Collections.emptyList());
        when(involvementRepository.findEmploymentsForPerson(11))
            .thenReturn(Collections.emptyList());

        // when
        var personIds = projectScopeCalculator.personIds(project);
        var organisationUnitIds = projectScopeCalculator.organisationUnitIds(project);

        // then
        assertEquals(List.of(11), personIds);
        assertEquals(List.of(22), organisationUnitIds);
    }

    @Test
    public void shouldAddTheInstitutionsThatEmployTheContributors() {
        // given
        var project = new Project();
        project.getPersons().add(personContribution(11));

        var employment = new Employment();
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        employment.setOrganisationUnit(organisationUnit(33));

        var otherInvolvement = new Employment();
        otherInvolvement.setInvolvementType(InvolvementType.HIRED_BY);
        otherInvolvement.setOrganisationUnit(organisationUnit(44));

        when(involvementRepository.findEmploymentsForPerson(11))
            .thenReturn(List.of(employment, otherInvolvement));
        when(organisationUnitService.getSuperOUsHierarchyRecursive(anyInt()))
            .thenReturn(Collections.emptyList());

        // when
        var organisationUnitIds = projectScopeCalculator.organisationUnitIds(project);

        // then
        assertEquals(List.of(33), organisationUnitIds);
    }

    @Test
    public void shouldAddTheFundersOfTheProjectsFundings() {
        // given
        var project = new Project();
        project.getFunding().addAll(List.of(funding(55), funding(null)));

        when(organisationUnitService.getSuperOUsHierarchyRecursive(anyInt()))
            .thenReturn(Collections.emptyList());

        // when
        var organisationUnitIds = projectScopeCalculator.organisationUnitIds(project);

        // then
        assertEquals(List.of(55), organisationUnitIds);
    }

    @Test
    public void shouldAnswerWithEveryInstitutionsSuperHierarchy() {
        // given
        var project = new Project();
        project.getOrganisations().add(organisationContribution(22));

        when(organisationUnitService.getSuperOUsHierarchyRecursive(22))
            .thenReturn(List.of(222, 2222));

        // when
        var organisationUnitIds = projectScopeCalculator.organisationUnitIds(project);

        // then
        assertEquals(3, organisationUnitIds.size());
        assertTrue(organisationUnitIds.containsAll(List.of(22, 222, 2222)));
    }

    @Test
    public void shouldNotRepeatAnInstitutionReachedTwice() {
        // given a unit that is both a consortium member and a funder
        var project = new Project();
        project.getOrganisations().add(organisationContribution(22));
        project.getFunding().add(funding(22));

        when(organisationUnitService.getSuperOUsHierarchyRecursive(22))
            .thenReturn(Collections.emptyList());

        // when
        var organisationUnitIds = projectScopeCalculator.organisationUnitIds(project);

        // then
        assertEquals(List.of(22), organisationUnitIds);
    }
}
