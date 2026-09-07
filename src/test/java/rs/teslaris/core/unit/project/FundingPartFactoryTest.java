package rs.teslaris.core.unit.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.common.Currency;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.model.project.ProjectEvent;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.util.FundingPartFactory;

@SpringBootTest
public class FundingPartFactoryTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private ProjectEventRepository projectEventRepository;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Mock
    private FundingApplicationRepository fundingApplicationRepository;

    @Mock
    private PersonProjectContributionRepository personProjectContributionRepository;

    @Mock
    private OrganisationUnitProjectContributionRepository
        organisationUnitProjectContributionRepository;

    @InjectMocks
    private FundingPartFactory fundingPartFactory;

    private FundingPartDTO fundingPartDTO;
    private Funding funding;
    private Currency currency;
    private Set<MultiLingualContent> description;


    @BeforeEach
    void setUp() {
        funding = new Funding();
        funding.setId(1);

        currency = new Currency();
        currency.setId(1);

        description = new HashSet<>();

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(100);

        fundingPartDTO = new FundingPartDTO();
        fundingPartDTO.setFundingId(1);
        fundingPartDTO.setDescription(List.of());
        fundingPartDTO.setAmount(monetaryAmountDTO);

        when(fundingRepository.findById(1)).thenReturn(Optional.of(funding));
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
    }

    @Test
    public void shouldBuildFundingPartWithProjectEventSuccessfully() {
        // given
        fundingPartDTO.setProjectEventId(1);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(1);

        when(projectEventRepository.findById(1)).thenReturn(Optional.of(projectEvent));

        // when
        var result = fundingPartFactory.buildFundingPart(fundingPartDTO);

        // then
        verify(fundingRepository).findById(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(projectEventRepository).findById(1);

        assertThat(result).isNotNull();
        assertThat(result.getFunding()).isEqualTo(funding);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(result.getAmount().getAmount()).isEqualTo(100);
        assertThat(result.getProjectEvent()).isEqualTo(projectEvent);
    }

    @Test
    public void shouldBuildFundingPartWithFundingApplicationSuccessfully() {
        // given
        fundingPartDTO.setFundingApplicationId(2);

        var fundingApplication = new FundingApplication();
        fundingApplication.setId(2);

        when(fundingApplicationRepository.findById(2)).thenReturn(Optional.of(fundingApplication));

        // when
        var result = fundingPartFactory.buildFundingPart(fundingPartDTO);

        // then
        verify(fundingApplicationRepository).findById(2);
        verify(projectEventRepository, never()).findById(anyInt());

        assertThat(result.getFundingApplication()).isEqualTo(fundingApplication);
    }

    @Test
    public void shouldBuildFundingPartWithProjectDocumentSuccessfully() {
        // given
        fundingPartDTO.setProjectDocumentId(3);

        var projectDocument = new ProjectDocument();
        projectDocument.setId(3);

        when(projectDocumentRepository.findById(3)).thenReturn(Optional.of(projectDocument));

        // when
        var result = fundingPartFactory.buildFundingPart(fundingPartDTO);

        // then
        verify(projectDocumentRepository).findById(3);

        assertThat(result.getProjectDocument()).isEqualTo(projectDocument);
    }

    @Test
    public void shouldBuildFundingPartWithPersonProjectContributionSuccessfully() {
        // given
        fundingPartDTO.setPersonProjectContributionId(4);

        var contribution = new PersonProjectContribution();
        contribution.setId(4);

        when(personProjectContributionRepository.findById(4)).thenReturn(Optional.of(contribution));

        // when
        var result = fundingPartFactory.buildFundingPart(fundingPartDTO);

        // then
        verify(personProjectContributionRepository).findById(4);

        assertThat(result.getPersonContribution()).isEqualTo(contribution);
    }

    @Test
    public void shouldBuildFundingPartWithOUProjectContributionSuccessfully() {
        // given
        fundingPartDTO.setOrganisationUnitProjectContributionId(4);

        var contribution = new OrganisationUnitProjectContribution();
        contribution.setId(4);

        when(organisationUnitProjectContributionRepository.findById(4)).thenReturn(
            Optional.of(contribution));

        // when
        var result = fundingPartFactory.buildFundingPart(fundingPartDTO);

        // then
        verify(organisationUnitProjectContributionRepository).findById(4);

        assertThat(result.getOrganisationUnitContribution()).isEqualTo(contribution);
    }

    @Test
    public void shouldThrowWhenTargetDoesNotExist() {
        // given
        fundingPartDTO.setProjectEventId(999);

        when(projectEventRepository.findById(999)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fundingPartFactory.buildFundingPart(fundingPartDTO))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldIgnoreTargetIdsWhenBuildingNestedPart() {
        // given
        fundingPartDTO.setProjectEventId(5);
        fundingPartDTO.setProjectDocumentId(5);

        // when
        var result = fundingPartFactory.buildNestedFundingPart(fundingPartDTO);

        // then
        verify(projectEventRepository, never()).findById(anyInt());
        verify(projectDocumentRepository, never()).findById(anyInt());

        assertThat(result.getFunding()).isEqualTo(funding);
        assertThat(result.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(result.getProjectEvent()).isNull();
        assertThat(result.getProjectDocument()).isNull();
    }

    @Test
    public void shouldInitializeAmountWhenNullOnExistingPart() {
        // given
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(1);
        existingFundingPart.setAmount(null);

        fundingPartDTO.setProjectEventId(1);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(1);

        when(projectEventRepository.findById(1)).thenReturn(Optional.of(projectEvent));

        // when
        fundingPartFactory.setCommonFields(existingFundingPart, fundingPartDTO);

        // then
        assertThat(existingFundingPart.getAmount()).isNotNull();
        assertThat(existingFundingPart.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(existingFundingPart.getAmount().getAmount()).isEqualTo(100);
        assertThat(existingFundingPart.getProjectEvent()).isEqualTo(projectEvent);
    }

    @Test
    public void shouldClearPreviousTargetWhenSwitchingFromEventToApplication() {
        // given
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(1);
        existingFundingPart.setAmount(new MonetaryAmount());

        var existingProjectEvent = new ProjectEvent();
        existingProjectEvent.setId(5);
        existingFundingPart.setProjectEvent(existingProjectEvent);

        fundingPartDTO.setFundingApplicationId(2);
        fundingPartDTO.setProjectEventId(null);

        var fundingApplication = new FundingApplication();
        fundingApplication.setId(2);

        when(fundingApplicationRepository.findById(2)).thenReturn(Optional.of(fundingApplication));

        // when
        fundingPartFactory.setCommonFields(existingFundingPart, fundingPartDTO);

        // then
        verify(fundingApplicationRepository).findById(2);
        verify(projectEventRepository, never()).findById(anyInt());

        assertThat(existingFundingPart.getProjectEvent()).isNull();
        assertThat(existingFundingPart.getFundingApplication()).isEqualTo(fundingApplication);
    }
}
