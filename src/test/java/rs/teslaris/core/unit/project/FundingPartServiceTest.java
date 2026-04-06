package rs.teslaris.core.unit.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
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
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
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
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.service.impl.funding.FundingPartServiceImpl;
import rs.teslaris.project.service.interfaces.funding.FundingApplicationService;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;
import rs.teslaris.project.service.interfaces.project.ProjectEventService;

@SpringBootTest
public class FundingPartServiceTest {

    @Mock
    private FundingPartRepository fundingPartRepository;

    @Mock
    private FundingService fundingService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ProjectEventService projectEventService;

    @Mock
    private ProjectDocumentService projectDocumentService;

    @Mock
    private FundingApplicationService fundingApplicationService;

    @Mock
    private PersonProjectContributionService personProjectContributionService;

    @Mock
    private OrganisationUnitProjectContributionService organisationUnitProjectContributionService;

    @InjectMocks
    private FundingPartServiceImpl fundingPartService;

    private FundingPartDTO fundingPartDTO;
    private Funding funding;
    private Currency currency;
    private Set<MultiLingualContent> description;
    private MonetaryAmountDTO monetaryAmountDTO;


    @BeforeEach
    void setUp() {
        funding = new Funding();
        funding.setId(1);

        currency = new Currency();
        currency.setId(1);

        description = new HashSet<>();

        monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(100);

        fundingPartDTO = new FundingPartDTO();
        fundingPartDTO.setFundingId(1);
        fundingPartDTO.setDescription(List.of());
        fundingPartDTO.setAmount(monetaryAmountDTO);
    }

    @Test
    public void shouldCreateFundingPartSuccessfully() {
        // given
        fundingPartDTO.setProjectEventId(1);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(1);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(projectEventService.findOne(1)).thenReturn(projectEvent);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(projectEventService).findOne(1);
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result).isNotNull();
        assertThat(result.getFunding()).isEqualTo(funding);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(result.getAmount().getAmount()).isEqualTo(100);
        assertThat(result.getProjectEvent()).isEqualTo(projectEvent);
    }

    @Test
    public void shouldCreateFundingPartWithFundingApplicationSuccessfully() {
        // given
        fundingPartDTO.setFundingApplicationId(2);

        var fundingApplication = new FundingApplication();
        fundingApplication.setId(2);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingApplicationService.findOne(2)).thenReturn(fundingApplication);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingApplicationService).findOne(2);
        verify(projectEventService, never()).findOne(anyInt());
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getFundingApplication()).isEqualTo(fundingApplication);
    }

    @Test
    public void shouldCreateFundingPartWithProjectDocumentSuccessfully() {
        // given
        fundingPartDTO.setProjectDocumentId(3);

        var projectDocument = new ProjectDocument();
        projectDocument.setId(3);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(projectDocumentService.findOne(3)).thenReturn(projectDocument);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(projectDocumentService).findOne(3);
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getProjectDocument()).isEqualTo(projectDocument);
    }

    @Test
    public void shouldCreateFundingPartWithPersonProjectContributionSuccessfully() {
        // given
        fundingPartDTO.setPersonProjectContributionId(4);

        var contribution = new PersonProjectContribution();
        contribution.setId(4);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(personProjectContributionService.findOne(4)).thenReturn(contribution);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(personProjectContributionService).findOne(4);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getPersonProjectContribution()).isEqualTo(contribution);
    }

    @Test
    public void shouldCreateFundingPartWithOUProjectContributionSuccessfully() {
        // given
        fundingPartDTO.setOrganisationUnitProjectContributionId(4);

        var contribution = new OrganisationUnitProjectContribution();
        contribution.setId(4);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(organisationUnitProjectContributionService.findOne(4)).thenReturn(contribution);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(organisationUnitProjectContributionService).findOne(4);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getOrganisationUnitProjectContribution()).isEqualTo(contribution);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingFundingPartWithNoReference() {
        // given
        fundingPartDTO.setFundingId(null);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);

        // when & then
        assertThatThrownBy(() -> fundingPartService.createFundingPart(fundingPartDTO))
            .isInstanceOf(ReferenceConstraintException.class)
            .hasMessageContaining("Funding part must belong to one of the following");

        verify(fundingPartRepository, never()).save(any(FundingPart.class));
    }

    @Test
    public void shouldInitializeCostsWhenNullInUpdate() {
        // given
        var fundingPartId = 1;
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(fundingPartId);
        existingFundingPart.setAmount(null);

        fundingPartDTO.setProjectEventId(1);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(1);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(
            Optional.of(existingFundingPart));
        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(projectEventService.findOne(1)).thenReturn(projectEvent);

        // when
        fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(projectEventService).findOne(1);
        verify(fundingPartRepository).save(existingFundingPart);

        assertThat(existingFundingPart.getAmount()).isNotNull();
        assertThat(existingFundingPart.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(existingFundingPart.getAmount().getAmount()).isEqualTo(100);
    }

    @Test
    public void shouldUpdateFundingPartSuccessfully() {
        // given
        var fundingPartId = 1;
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(fundingPartId);

        var existingCosts = new MonetaryAmount();
        existingFundingPart.setAmount(existingCosts);

        fundingPartDTO.setProjectEventId(1);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(1);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(
            Optional.of(existingFundingPart));
        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(projectEventService.findOne(1)).thenReturn(projectEvent);

        // when
        fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(projectEventService).findOne(1);
        verify(fundingPartRepository).save(existingFundingPart);

        assertThat(existingFundingPart.getFunding()).isEqualTo(funding);
        assertThat(existingFundingPart.getDescription()).isEqualTo(description);
        assertThat(existingFundingPart.getAmount().getCurrency()).isEqualTo(currency);
        assertThat(existingFundingPart.getAmount().getAmount()).isEqualTo(100);
        assertThat(existingFundingPart.getProjectEvent()).isEqualTo(projectEvent);
    }

    @Test
    public void shouldDeleteFundingPartSuccessfully() {
        // given
        var fundingPartId = 1;
        var fundingPart = new FundingPart();
        fundingPart.setId(fundingPartId);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.of(fundingPart));
        doNothing().when(fundingPartRepository).delete(fundingPart);

        // when
        fundingPartService.deleteFundingPart(fundingPartId);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository).save(fundingPart);
    }

    @Test
    public void shouldThrowExceptionWhenDeletingNonExistentFundingPart() {
        // given
        var fundingPartId = 999;

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fundingPartService.deleteFundingPart(fundingPartId))
            .isInstanceOf(NotFoundException.class);

        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository, never()).delete(any(FundingPart.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFundingPart() {
        // given
        var fundingPartId = 999;

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
            () -> fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO))
            .isInstanceOf(NotFoundException.class);

        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository, never()).save(any(FundingPart.class));
    }

    @Test
    public void shouldUpdateFundingPartFromEventToProposalSuccessfully() {
        // given
        var fundingPartId = 1;
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(fundingPartId);

        var existingProjectEvent = new ProjectEvent();
        existingProjectEvent.setId(5);
        existingFundingPart.setProjectEvent(existingProjectEvent);

        var existingCosts = new MonetaryAmount();
        existingFundingPart.setAmount(existingCosts);

        fundingPartDTO.setFundingApplicationId(2);
        fundingPartDTO.setProjectEventId(null);

        var fundingApplication = new FundingApplication();
        fundingApplication.setId(2);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(
            Optional.of(existingFundingPart));
        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingApplicationService.findOne(2)).thenReturn(fundingApplication);

        // when
        fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingApplicationService).findOne(2);
        verify(projectEventService, never()).findOne(anyInt());
        verify(fundingPartRepository).save(existingFundingPart);

        assertThat(existingFundingPart.getProjectEvent()).isNull();
        assertThat(existingFundingPart.getFundingApplication()).isEqualTo(fundingApplication);
    }
}
