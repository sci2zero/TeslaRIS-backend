package rs.teslaris.core.unit;

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
import rs.teslaris.core.dto.project.FundingPartDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.project.Currency;
import rs.teslaris.core.model.project.Funding;
import rs.teslaris.core.model.project.FundingPart;
import rs.teslaris.core.model.project.FundingProposal;
import rs.teslaris.core.model.project.MonetaryAmount;
import rs.teslaris.core.model.project.ProjectDocument;
import rs.teslaris.core.model.project.ProjectEvent;
import rs.teslaris.core.repository.project.FundingPartRepository;
import rs.teslaris.core.service.impl.project.FundingPartServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.project.FundingProposalService;
import rs.teslaris.core.service.interfaces.project.FundingService;
import rs.teslaris.core.service.interfaces.project.ProjectDocumentService;
import rs.teslaris.core.service.interfaces.project.ProjectEventService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;

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
    private FundingProposalService fundingProposalService;

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
        fundingPartDTO.setCosts(monetaryAmountDTO);
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
        assertThat(result.getCosts().getCurrency()).isEqualTo(currency);
        assertThat(result.getCosts().getAmount()).isEqualTo(100);
        assertThat(result.getProjectEvent()).isEqualTo(projectEvent);
    }

    @Test
    public void shouldCreateFundingPartWithFundingProposalSuccessfully() {
        // given
        fundingPartDTO.setFundingProposalId(2);

        var fundingProposal = new FundingProposal();
        fundingProposal.setId(2);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingProposalService.findOne(2)).thenReturn(fundingProposal);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingProposalService).findOne(2);
        verify(projectEventService, never()).findOne(anyInt());
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getFundingProposal()).isEqualTo(fundingProposal);
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
    public void shouldCreateFundingPartWithForFundingSuccessfully() {
        // given
        fundingPartDTO.setForFundingId(4);

        var forFunding = new Funding();
        forFunding.setId(4);

        when(fundingService.findOne(1)).thenReturn(funding);
        when(fundingService.findOne(4)).thenReturn(forFunding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingService).findOne(1);
        verify(fundingService).findOne(4);
        verify(multilingualContentService).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingPartRepository).save(any(FundingPart.class));

        assertThat(result.getForFunding()).isEqualTo(forFunding);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingFundingPartWithNoReference() {
        // given
        when(fundingService.findOne(1)).thenReturn(funding);
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
        existingFundingPart.setCosts(null);

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

        assertThat(existingFundingPart.getCosts()).isNotNull();
        assertThat(existingFundingPart.getCosts().getCurrency()).isEqualTo(currency);
        assertThat(existingFundingPart.getCosts().getAmount()).isEqualTo(100);
    }

    @Test
    public void shouldUpdateFundingPartSuccessfully() {
        // given
        var fundingPartId = 1;
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(fundingPartId);

        var existingCosts = new MonetaryAmount();
        existingFundingPart.setCosts(existingCosts);

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
        assertThat(existingFundingPart.getCosts().getCurrency()).isEqualTo(currency);
        assertThat(existingFundingPart.getCosts().getAmount()).isEqualTo(100);
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
        existingFundingPart.setCosts(existingCosts);

        fundingPartDTO.setFundingProposalId(2);
        fundingPartDTO.setProjectEventId(null);

        var fundingProposal = new FundingProposal();
        fundingProposal.setId(2);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(
            Optional.of(existingFundingPart));
        when(fundingService.findOne(1)).thenReturn(funding);
        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(description);
        when(currencyService.findOne(1)).thenReturn(currency);
        when(fundingProposalService.findOne(2)).thenReturn(fundingProposal);

        // when
        fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingProposalService).findOne(2);
        verify(projectEventService, never()).findOne(anyInt());
        verify(fundingPartRepository).save(existingFundingPart);

        assertThat(existingFundingPart.getProjectEvent()).isNull();
        assertThat(existingFundingPart.getFundingProposal()).isEqualTo(fundingProposal);
    }
}
