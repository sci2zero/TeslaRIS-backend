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
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.service.impl.project.OrganisationUnitProjectContributionServiceImpl;

@SpringBootTest
public class OrganisationUnitProjectContributionServiceTest {

    @Mock
    private OrganisationUnitProjectContributionRepository
        organisationUnitProjectContributionRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private PersonService personService;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private OrganisationUnitProjectContributionServiceImpl contributionService;

    private static OrganisationUnitProjectContributionDTO contributionDTO() {
        var dto = new OrganisationUnitProjectContributionDTO();
        dto.setContributionType(OrganisationUnitProjectContributionType.PARTNER);
        dto.setOrderNumber(1);
        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2026, 3, 1));
        dto.setUris(Set.of("https://example.com/proof"));
        dto.setContributionDescription(List.of());
        dto.setDisplayProject(List.of());
        return dto;
    }

    @Test
    public void shouldCreateContributionWithOrganisationUnit() {
        // given
        var project = new Project();
        project.setId(1);

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(5);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(5);
        dto.setIsMainContributor(true);
        dto.setFavorite(true);

        when(organisationUnitService.findOne(5)).thenReturn(organisationUnit);
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertNotNull(result);
        assertEquals(organisationUnit, result.getOrganisationUnit());
        assertEquals(project, result.getProject());
        assertEquals(OrganisationUnitProjectContributionType.PARTNER,
            result.getContributionType());
        assertEquals(1, result.getOrderNumber());
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        assertEquals(LocalDate.of(2025, 1, 1), result.getDateFrom());
        assertTrue(result.isMainContributor());
        assertTrue(result.getFavorite());
        assertTrue(result.getDisplayOrganisationUnit().isEmpty());
        verify(organisationUnitService).findOne(5);
    }

    @Test
    public void shouldCreateContributionWithDisplayNameWhenNoOrganisationUnitId() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);
        dto.setDisplayOrganisationUnit(List.of());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertNull(result.getOrganisationUnit());
        assertFalse(result.getDisplayOrganisationUnit().isEmpty());
        verify(organisationUnitService, never()).findOne(anyInt());
    }

    @Test
    public void shouldDefaultFlagsToFalseWhenNotProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);
        dto.setIsMainContributor(null);
        dto.setFavorite(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertFalse(result.isMainContributor());
        assertFalse(result.getFavorite());
    }

    @Test
    public void shouldResolveContactPersonWhenProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var contactPerson = new Person();
        contactPerson.setId(7);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);
        dto.setContactPersonId(7);

        when(personService.findOne(7)).thenReturn(contactPerson);
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertEquals(contactPerson, result.getContactPerson());
        verify(personService).findOne(7);
    }

    @Test
    public void shouldNotResolveContactPersonWhenNotProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);
        dto.setContactPersonId(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertNull(result.getContactPerson());
        verify(personService, never()).findOne(anyInt());
    }

    @Test
    public void shouldBuildFundingParts() {
        // given
        var project = new Project();
        project.setId(1);

        var fundingPart = new FundingPartDTO();
        fundingPart.setDescription(List.of());
        fundingPart.setAmount(new MonetaryAmountDTO(1, 50000));

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);
        dto.setFundingParts(List.of(fundingPart));

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(currencyService.findOne(1)).thenReturn(null);
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertEquals(1, result.getFundingParts().size());
        var part = result.getFundingParts().iterator().next();
        assertEquals(result, part.getOrganisationUnitContribution());
        assertEquals(50000.0, part.getAmount().getAmount());
        verify(currencyService).findOne(1);
    }

    @Test
    public void shouldCreateContributionWithoutFundingPartsWhenNoneProvided() {
        // given
        var project = new Project();
        project.setId(1);

        var dto = contributionDTO();
        dto.setOrganisationUnitId(null);

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitProjectContributionRepository.save(
            any(OrganisationUnitProjectContribution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = contributionService.createContribution(dto, project);

        // then
        assertTrue(result.getFundingParts().isEmpty());
        verify(currencyService, never()).findOne(anyInt());
    }
}
