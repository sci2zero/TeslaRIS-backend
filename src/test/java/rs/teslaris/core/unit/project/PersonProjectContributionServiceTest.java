package rs.teslaris.core.unit.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.PersonProjectContributionType;
import rs.teslaris.project.model.project.PersonProjectInvestigationRole;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.service.impl.project.PersonProjectContributionServiceImpl;

@SpringBootTest
public class PersonProjectContributionServiceTest {

    @Mock
    private PersonProjectContributionRepository personProjectContributionRepository;

    @Mock
    private PersonService personService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ResearchAreaService researchAreaService;

    @InjectMocks
    private PersonProjectContributionServiceImpl contributionService;

    private static PersonProjectContributionDTO contributionDTO() {
        var dto = new PersonProjectContributionDTO();
        dto.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        dto.setInvestigationRole(PersonProjectInvestigationRole.RESEARCHER);
        dto.setOrderNumber(1);
        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2026, 3, 1));
        dto.setUris(Set.of("https://example.com/proof"));
        dto.setContributionDescription(List.of());
        dto.setDisplayAffiliationStatement(List.of());
        dto.setKeywords(List.of());
        dto.setOtherRoleDescription(List.of());
        dto.setDisplayProject(List.of());
        dto.setFavorite(true);
        return dto;
    }

    private static Person personWithName() {
        var person = new Person();
        person.setId(3);
        person.setName(new PersonName("Ana", "", "Anić",
            LocalDate.of(1990, 1, 1), null, PersonNameType.CITATION_NAME));
        return person;
    }

    @Test
    public void shouldCreateContributionWithPerson() {
        // given
        var project = new Project();
        project.setId(1);

        var person = personWithName();

        var institution = new OrganisationUnit();
        institution.setId(5);

        var dto = contributionDTO();
        dto.setPersonId(3);
        dto.setInstitutionIds(List.of(5));
        dto.setIsMainContributor(true);
        dto.setIsInvitedContributor(false);

        when(personService.findOne(3)).thenReturn(person);
        when(organisationUnitService.findOne(5)).thenReturn(institution);
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertNotNull(result);
        assertEquals(person, result.getPerson());
        assertEquals(project, result.getProject());
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        assertEquals(PersonProjectContributionType.TEAM_MEMBER, result.getContributionType());
        assertEquals(PersonProjectInvestigationRole.RESEARCHER, result.getInvestigationRole());
        assertEquals(1, result.getOrderNumber());
        assertEquals(1, result.getInstitutions().size());
        assertEquals(LocalDate.of(2025, 1, 1), result.getDateFrom());
        assertTrue(result.getIsMainContributor());
        assertFalse(result.getIsInvitedContributor());
        verify(personService).findOne(3);
        verify(organisationUnitService).findOne(5);
    }

    @Test
    public void shouldCreateExternalContributionWhenPersonIdIsNull() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setPersonId(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertNull(result.getPerson());
        assertNotNull(result.getAffiliationStatement());
        verify(personService, never()).findOne(anyInt());
    }

    @Test
    public void shouldDefaultContributorFlagsToFalseWhenNotProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setPersonId(null);
        dto.setIsMainContributor(null);
        dto.setIsInvitedContributor(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertFalse(result.getIsMainContributor());
        assertFalse(result.getIsInvitedContributor());
    }

    @Test
    public void shouldFallBackToContributorNameWhenDisplayNameNotProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var person = personWithName();

        var dto = contributionDTO();
        dto.setPersonId(3);
        dto.setPersonName(null);

        when(personService.findOne(3)).thenReturn(person);
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        var displayName = result.getAffiliationStatement().getDisplayPersonName();
        assertEquals("Ana", displayName.getFirstname());
        assertEquals("Anić", displayName.getLastname());
        assertEquals(PersonNameType.CITATION_NAME, displayName.getNameType());
    }

    @Test
    public void shouldBuildFundingPartsAndResearchAreas() {
        // given
        var project = new Project();
        project.setId(1);

        var fundingPart = new FundingPartDTO();
        fundingPart.setDescription(List.of());
        fundingPart.setAmount(new MonetaryAmountDTO(1, 50000));

        var dto = contributionDTO();
        dto.setPersonId(null);
        dto.setFundingParts(List.of(fundingPart));
        dto.setResearchAreasId(Set.of(2));

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(currencyService.findOne(1)).thenReturn(null);
        when(researchAreaService.getResearchAreasByIds(anyList())).thenReturn(List.of());
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertEquals(1, result.getFundingParts().size());
        assertEquals(50000.0, result.getFundingParts().iterator().next().getAmount().getAmount());
        verify(currencyService).findOne(1);
        verify(researchAreaService).getResearchAreasByIds(anyList());
    }

    @Test
    public void shouldNotResolveResearchAreasWhenNoneProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setPersonId(null);
        dto.setResearchAreasId(Set.of());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(personProjectContributionRepository.save(any(PersonProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertTrue(result.getResearchAreas().isEmpty());
        verify(researchAreaService, never()).getResearchAreasByIds(anyList());
    }
}
