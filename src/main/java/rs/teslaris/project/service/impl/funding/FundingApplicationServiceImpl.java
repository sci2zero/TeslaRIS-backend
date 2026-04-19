package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.project.converter.funding.FundingApplicationConverter;
import rs.teslaris.project.converter.funding.FundingCallConverter;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.indexmodel.funding.FundingApplicationIndex;
import rs.teslaris.project.indexrepository.funding.FundingApplicationIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.service.interfaces.funding.FundingApplicationService;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingService;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class FundingApplicationServiceImpl extends JPAServiceImpl<FundingApplication>
    implements FundingApplicationService {

    private final FundingApplicationRepository fundingApplicationRepository;

    private final FundingApplicationIndexRepository fundingApplicationIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final CurrencyService currencyService;

    private final FundingCallService fundingCallService;

    private final FundingService fundingService;

    @Override
    protected JpaRepository<FundingApplication, Integer> getEntityRepository() {
        return fundingApplicationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FundingApplicationDTO readFundingApplication(Integer fundingApplicationId) {
        return FundingApplicationConverter.toDTO(findOne(fundingApplicationId));
    }

    @Override
    @Transactional
    public FundingApplication createFundingApplication(FundingApplicationDTO fundingApplicationDTO) {
        var newApplication = new FundingApplication();

        setCommonFields(newApplication, fundingApplicationDTO);

        var saved = save(newApplication);

        fundingApplicationIndexRepository.save(
                indexCommonFields(saved, new FundingApplicationIndex()));

        return saved;
    }

    @Override
    @Transactional
    public void updateFundingApplication(Integer fundingApplicationId, FundingApplicationDTO fundingApplicationDTO) {
        var applicationToUpdate = findOne(fundingApplicationId);

        clearCommonFields(applicationToUpdate);
        setCommonFields(applicationToUpdate, fundingApplicationDTO);

        save(applicationToUpdate);

        fundingApplicationIndexRepository
                .findFundingApplicationIndexByDatabaseId(fundingApplicationId)
                .ifPresent(index -> {
                    indexCommonFields(applicationToUpdate, index);
                    fundingApplicationIndexRepository.save(index);
                });
    }

    @Override
    @Transactional
    public void deleteFundingApplication(Integer fundingApplicationId) {
        if (fundingApplicationRepository.isRevisedByOther(fundingApplicationId)) {
            throw new ReferenceConstraintException(
                    "fundingApplicationIsRevisedMessage");
        }

        if (fundingApplicationRepository.hasFunding(fundingApplicationId)) {
            throw new ReferenceConstraintException(
                    "fundingApplicationHasFundingMessage");
        }

        delete(fundingApplicationId);

        fundingApplicationIndexRepository
                .findFundingApplicationIndexByDatabaseId(fundingApplicationId)
                .ifPresent(fundingApplicationIndexRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexFundingApplications() {
        FunctionalUtil.processAllPages(
                100,
                Sort.by(Sort.Direction.ASC, "id"),
                this::findAll,
                fundingApplication -> indexFundingApplication(fundingApplication, new FundingApplicationIndex())
        );

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void indexFundingApplication(FundingApplication fundingApplication, FundingApplicationIndex index) {
        indexCommonFields(fundingApplication, index);
        fundingApplicationIndexRepository.save(index);
    }

    private void setCommonFields(FundingApplication application,
                                 FundingApplicationDTO dto) {
        if (Objects.isNull(dto.getFundingCallId())) {
            throw new ReferenceConstraintException(
                    "Funding application must be bound to a funding call.");
        }

        var fundingCall = fundingCallService.findOne(dto.getFundingCallId());
        application.setFundingCall(fundingCall);

        validateDateChain(dto);
        validateSubmissionWithinCall(dto, fundingCall);

        application.setProject(null); // ToDO change when project is finished

        if (Objects.nonNull(dto.getRevisedFundingApplicationId())) {
            application.setRevisedFundingApplication(
                    findOne(dto.getRevisedFundingApplicationId()));
        } else {
            application.setRevisedFundingApplication(null);
        }

        if (Objects.nonNull(dto.getFundingId())) {
            application.setFunding(fundingService.findOne(dto.getFundingId()));
        } else {
            application.setFunding(null);
        }

        application.setDescription(
                multilingualContentService.getMultilingualContent(dto.getDescription()));
        application.setResponseSummary(
                multilingualContentService.getMultilingualContent(dto.getResponseSummary()));

        if (Objects.nonNull(dto.getRequestedAmount())) {
            if (Objects.isNull(application.getRequestedAmount())) {
                application.setRequestedAmount(new MonetaryAmount());
            }
            application.getRequestedAmount().setCurrency(
                    currencyService.findOne(dto.getRequestedAmount().getCurrencyId()));
            application.getRequestedAmount().setAmount(dto.getRequestedAmount().getAmount());
        } else {
            application.setRequestedAmount(null);
        }

        application.setSubmissionDate(dto.getSubmissionDate());
        application.setReviewDateFrom(dto.getReviewDateFrom());
        application.setReviewDateTo(dto.getReviewDateTo());
        application.setDecisionDate(dto.getDecisionDate());
        application.setRevisedProposalOrNextRoundDeadlineDate(
                dto.getRevisedProposalOrNextRoundDeadlineDate());
        application.setResult(dto.getResult());

        rebuildOtherFundingSources(application, dto);
    }

    private void rebuildOtherFundingSources(FundingApplication application,
                                            FundingApplicationDTO dto) {
        if (Objects.isNull(application.getOtherFundingSources())) {
            application.setOtherFundingSources(new HashSet<>());
        }

        dto.getOtherFundingSources().forEach(partDto -> {
            var part = buildFundingPart(partDto, application);
            application.getOtherFundingSources().add(part);
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO partDto, FundingApplication parent) {
        var part = new FundingPart();

        part.setDescription(
                multilingualContentService.getMultilingualContent(partDto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
                currencyService.findOne(partDto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(partDto.getAmount().getAmount());

        if (Objects.nonNull(partDto.getFundingId())) {
            part.setFunding(fundingService.findOne(partDto.getFundingId()));
        }

        part.setFundingApplication(parent);

        return part;
    }

    private void validateDateChain(FundingApplicationDTO dto) {
        var sub = dto.getSubmissionDate();
        var revFrom = dto.getReviewDateFrom();
        var revTo = dto.getReviewDateTo();
        var dec = dto.getDecisionDate();

        if (Objects.nonNull(sub) && Objects.nonNull(revFrom) && revFrom.isBefore(sub)) {
            throw new DateRangeException(
                    "Review start date must be on or after submission date.");
        }
        if (Objects.nonNull(revFrom) && Objects.nonNull(revTo) && revTo.isBefore(revFrom)) {
            throw new DateRangeException(
                    "Review end date must be on or after review start date.");
        }
        if (Objects.nonNull(revTo) && Objects.nonNull(dec) && dec.isBefore(revTo)) {
            throw new DateRangeException(
                    "Decision date must be on or after review end date.");
        }
        if (Objects.nonNull(sub) && Objects.nonNull(dec) && dec.isBefore(sub)) {
            throw new DateRangeException(
                    "Decision date must be on or after submission date.");
        }
    }

    private void validateSubmissionWithinCall(FundingApplicationDTO dto, FundingCall call) {
        if (Objects.isNull(dto.getSubmissionDate())) {
            return;
        }
        if (Objects.nonNull(call.getDateFrom()) &&
                dto.getSubmissionDate().isBefore(call.getDateFrom())) {
            throw new DateRangeException(
                    "Submission date must be on or after funding call opening.");
        }
        if (Objects.nonNull(call.getDateTo()) &&
                dto.getSubmissionDate().isAfter(call.getDateTo())) {
            throw new DateRangeException(
                    "Submission date must be on or before funding call closing.");
        }
    }

    private void clearCommonFields(FundingApplication application) {
        application.getDescription().clear();
        application.getResponseSummary().clear();
        if (Objects.nonNull(application.getOtherFundingSources())) {
            application.getOtherFundingSources().clear();
        }
    }

    private FundingApplicationIndex indexCommonFields(FundingApplication application,
                                                      FundingApplicationIndex index) {
        index.setDatabaseId(application.getId());
        index.setFundingCallId(application.getFundingCall().getId());

        if (Objects.nonNull(application.getProject())) {
            index.setProjectId(application.getProject().getId());
        }
        if (Objects.nonNull(application.getFundingCall().getFunder())) {
            index.setFunderId(application.getFundingCall().getFunder().getId());
        }

        index.setSubmissionDate(application.getSubmissionDate());
        index.setDecisionDate(application.getDecisionDate());

        if (Objects.nonNull(application.getResult())) {
            index.setResult(application.getResult().name());
        }

        return index;
    }
}
