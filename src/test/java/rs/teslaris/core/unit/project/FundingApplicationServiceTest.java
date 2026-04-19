package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.indexmodel.funding.FundingApplicationIndex;
import rs.teslaris.project.indexrepository.funding.FundingApplicationIndexRepository;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.model.funding.FundingApplicationResult;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.service.impl.funding.FundingApplicationServiceImpl;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FundingApplicationServiceTest {
    @Mock
    private FundingApplicationRepository fundingApplicationRepository;

    @Mock
    private FundingApplicationIndexRepository fundingApplicationIndexRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private FundingCallService fundingCallService;

    @Mock
    private FundingService fundingService;

    @InjectMocks
    private FundingApplicationServiceImpl fundingApplicationService;

    private FundingCall createTestFundingCall() {
        var fundingCall = new FundingCall();
        fundingCall.setId(1);
        fundingCall.setDateFrom(LocalDate.of(2020, 1, 1));
        fundingCall.setDateTo(LocalDate.of(2021, 1, 1));
        fundingCall.setFunder(new OrganisationUnit());
        return fundingCall;
    }

    private FundingApplicationDTO createTestFundingApplicationDTO() {
        var dto = new FundingApplicationDTO();
        dto.setFundingCallId(1);
        dto.setDescription(List.of());
        dto.setResponseSummary(List.of());
        dto.setOtherFundingSources(List.of());
        dto.setSubmissionDate(LocalDate.of(2020, 6, 15));
        dto.setReviewDateFrom(LocalDate.of(2020, 7, 1));
        dto.setReviewDateTo(LocalDate.of(2020, 7, 31));
        dto.setDecisionDate(LocalDate.of(2020, 8, 15));
        dto.setResult(FundingApplicationResult.AWARDED);
        return dto;
    }

    private FundingApplication createTestFundingApplication(Integer id) {
        var application = new FundingApplication();
        application.setId(id);
        application.setFundingCall(createTestFundingCall());
        application.setDescription(new HashSet<>());
        application.setResponseSummary(new HashSet<>());
        application.setOtherFundingSources(new HashSet<>());
        return application;
    }

    @Test
    public void shouldReturnFundingApplicationDTOWhenFundingApplicationExists() {
        // given
        var fundingApplicationId = 1;
        var fundingApplication = new FundingApplication();
        fundingApplication.setId(fundingApplicationId);
        fundingApplication.setOtherFundingSources(new HashSet<>());

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.of(fundingApplication));

        // when
        var result = fundingApplicationService.readFundingApplication(fundingApplicationId);

        // then
        assertNotNull(result);
        assertEquals(fundingApplicationId, result.getId());
        verify(fundingApplicationRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingApplicationNotFound() {
        // given
        var fundingApplicationId = 999;

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingApplicationService.readFundingApplication(fundingApplicationId));
        verify(fundingApplicationRepository).findById(fundingApplicationId);
    }

    @Test
    public void shouldCreateFundingApplicationSuccessfully() {
        // given
        var dto = createTestFundingApplicationDTO();

        var monetaryAmount = new MonetaryAmountDTO();
        monetaryAmount.setAmount(50000.0);
        monetaryAmount.setCurrencyId(1);
        dto.setRequestedAmount(monetaryAmount);

        var savedApplication = createTestFundingApplication(1);

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(currencyService.findOne(1)).thenReturn(null);
        when(fundingApplicationRepository.save(any(FundingApplication.class)))
                .thenReturn(savedApplication);
        when(fundingApplicationIndexRepository.save(any(FundingApplicationIndex.class)))
                .thenReturn(new FundingApplicationIndex());

        // when
        var result = fundingApplicationService.createFundingApplication(dto);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(fundingCallService).findOne(1);
        verify(multilingualContentService, times(2)).getMultilingualContent(anyList());
        verify(currencyService).findOne(1);
        verify(fundingApplicationRepository).save(any(FundingApplication.class));
        verify(fundingApplicationIndexRepository).save(any(FundingApplicationIndex.class));
    }

    @Test
    public void shouldCreateFundingApplicationWithoutMonetaryAmount() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setRequestedAmount(null);

        var savedApplication = createTestFundingApplication(1);

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(fundingApplicationRepository.save(any(FundingApplication.class)))
                .thenReturn(savedApplication);
        when(fundingApplicationIndexRepository.save(any(FundingApplicationIndex.class)))
                .thenReturn(new FundingApplicationIndex());

        // when
        var result = fundingApplicationService.createFundingApplication(dto);

        // then
        assertNotNull(result);
        verify(currencyService, never()).findOne(anyInt());
    }

    @Test
    public void shouldCreateFundingApplicationWithOtherFundingSources() {
        // given
        var dto = createTestFundingApplicationDTO();

        var partDto = new FundingPartDTO();
        partDto.setFundingId(1);
        partDto.setDescription(List.of());
        var partAmount = new MonetaryAmountDTO();
        partAmount.setAmount(10000.0);
        partAmount.setCurrencyId(1);
        partDto.setAmount(partAmount);
        dto.setOtherFundingSources(List.of(partDto));

        var savedApplication = createTestFundingApplication(1);

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(currencyService.findOne(1)).thenReturn(null);
        when(fundingService.findOne(1)).thenReturn(null);
        when(fundingApplicationRepository.save(any(FundingApplication.class)))
                .thenReturn(savedApplication);
        when(fundingApplicationIndexRepository.save(any(FundingApplicationIndex.class)))
                .thenReturn(new FundingApplicationIndex());

        // when
        var result = fundingApplicationService.createFundingApplication(dto);

        // then
        assertNotNull(result);
        verify(fundingService).findOne(1);
        verify(multilingualContentService, times(3)).getMultilingualContent(anyList());
    }

    @Test
    public void shouldThrowWhenFundingCallIdIsNull() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setFundingCallId(null);

        // when & then
        assertThrows(ReferenceConstraintException.class,
                () -> fundingApplicationService.createFundingApplication(dto));

        verify(fundingCallService, never()).findOne(any());
        verify(fundingApplicationRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenReviewStartBeforeSubmission() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setSubmissionDate(LocalDate.of(2020, 8, 1));
        dto.setReviewDateFrom(LocalDate.of(2020, 7, 1));

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldThrowWhenReviewEndBeforeReviewStart() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setReviewDateFrom(LocalDate.of(2020, 8, 1));
        dto.setReviewDateTo(LocalDate.of(2020, 7, 1));

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldThrowWhenDecisionBeforeReviewEnd() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setReviewDateTo(LocalDate.of(2020, 9, 1));
        dto.setDecisionDate(LocalDate.of(2020, 8, 1));

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldThrowWhenDecisionBeforeSubmission() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setSubmissionDate(LocalDate.of(2020, 9, 1));
        dto.setReviewDateFrom(null);
        dto.setReviewDateTo(null);
        dto.setDecisionDate(LocalDate.of(2020, 8, 1));

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldThrowWhenSubmissionBeforeCallOpening() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setSubmissionDate(LocalDate.of(2019, 6, 1));

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldThrowWhenSubmissionAfterCallClosing() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setSubmissionDate(LocalDate.of(2022, 1, 1));
        dto.setReviewDateFrom(null);
        dto.setReviewDateTo(null);
        dto.setDecisionDate(null);

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());

        // when & then
        assertThrows(DateRangeException.class,
                () -> fundingApplicationService.createFundingApplication(dto));
    }

    @Test
    public void shouldCreateSuccessfullyWhenSubmissionDateIsNull() {
        // given
        var dto = createTestFundingApplicationDTO();
        dto.setSubmissionDate(null);
        dto.setReviewDateFrom(null);
        dto.setReviewDateTo(null);
        dto.setDecisionDate(null);

        var savedApplication = createTestFundingApplication(1);

        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(fundingApplicationRepository.save(any(FundingApplication.class)))
                .thenReturn(savedApplication);
        when(fundingApplicationIndexRepository.save(any(FundingApplicationIndex.class)))
                .thenReturn(new FundingApplicationIndex());

        // when
        var result = fundingApplicationService.createFundingApplication(dto);

        // then
        assertNotNull(result);
    }

    @Test
    public void shouldUpdateFundingApplicationSuccessfully() {
        // given
        var fundingApplicationId = 1;
        var existingApplication = createTestFundingApplication(fundingApplicationId);

        var dto = createTestFundingApplicationDTO();

        var fundingApplicationIndex = new FundingApplicationIndex();
        fundingApplicationIndex.setDatabaseId(fundingApplicationId);

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.of(existingApplication));
        when(fundingCallService.findOne(1)).thenReturn(createTestFundingCall());
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(fundingApplicationRepository.save(any(FundingApplication.class)))
                .thenReturn(existingApplication);
        when(fundingApplicationIndexRepository.findFundingApplicationIndexByDatabaseId(fundingApplicationId))
                .thenReturn(Optional.of(fundingApplicationIndex));

        // when
        fundingApplicationService.updateFundingApplication(fundingApplicationId, dto);

        // then
        verify(fundingApplicationRepository).findById(fundingApplicationId);
        verify(fundingCallService).findOne(1);
        verify(multilingualContentService, times(2)).getMultilingualContent(anyList());
        verify(fundingApplicationRepository).save(any(FundingApplication.class));
        verify(fundingApplicationIndexRepository).findFundingApplicationIndexByDatabaseId(fundingApplicationId);
        verify(fundingApplicationIndexRepository).save(any(FundingApplicationIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFundingApplication() {
        // given
        var fundingApplicationId = 999;
        var dto = new FundingApplicationDTO();

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingApplicationService.updateFundingApplication(fundingApplicationId, dto));
        verify(fundingApplicationRepository).findById(fundingApplicationId);
        verify(fundingApplicationIndexRepository, never())
                .findFundingApplicationIndexByDatabaseId(anyInt());
    }

    @Test
    public void shouldDeleteFundingApplicationSuccessfully() {
        // given
        var fundingApplicationId = 1;

        when(fundingApplicationRepository.isRevisedByOther(fundingApplicationId))
                .thenReturn(false);
        when(fundingApplicationRepository.hasFunding(fundingApplicationId))
                .thenReturn(false);
        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.of(new FundingApplication()));
        when(fundingApplicationIndexRepository.findFundingApplicationIndexByDatabaseId(fundingApplicationId))
                .thenReturn(Optional.of(new FundingApplicationIndex()));

        // when
        fundingApplicationService.deleteFundingApplication(fundingApplicationId);

        // then
        verify(fundingApplicationRepository).isRevisedByOther(fundingApplicationId);
        verify(fundingApplicationRepository).hasFunding(fundingApplicationId);
        verify(fundingApplicationRepository).save(any());
        verify(fundingApplicationIndexRepository).delete(any(FundingApplicationIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenDeletingRevisedFundingApplication() {
        // given
        var fundingApplicationId = 1;

        when(fundingApplicationRepository.isRevisedByOther(fundingApplicationId))
                .thenReturn(true);

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
                fundingApplicationService.deleteFundingApplication(fundingApplicationId));
        verify(fundingApplicationRepository).isRevisedByOther(fundingApplicationId);
        verify(fundingApplicationRepository, never()).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenDeletingFundingApplicationWithFunding() {
        // given
        var fundingApplicationId = 1;

        when(fundingApplicationRepository.isRevisedByOther(fundingApplicationId))
                .thenReturn(false);
        when(fundingApplicationRepository.hasFunding(fundingApplicationId))
                .thenReturn(true);

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
                fundingApplicationService.deleteFundingApplication(fundingApplicationId));
        verify(fundingApplicationRepository).hasFunding(fundingApplicationId);
        verify(fundingApplicationRepository, never()).save(any());
    }

    @Test
    public void shouldIndexSingleFundingApplication() {
        // given
        var fundingApplication = createTestFundingApplication(1);
        var fundingApplicationIndex = new FundingApplicationIndex();

        when(fundingApplicationIndexRepository.save(any(FundingApplicationIndex.class)))
                .thenReturn(fundingApplicationIndex);

        // when
        fundingApplicationService.indexFundingApplication(fundingApplication, fundingApplicationIndex);

        // then
        verify(fundingApplicationIndexRepository).save(any(FundingApplicationIndex.class));
    }
}
