package rs.teslaris.project.converter.funding;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.model.funding.FundingApplication;

import java.util.Objects;
import java.util.stream.Collectors;

public class FundingApplicationConverter {

    public static FundingApplicationDTO toDTO(FundingApplication fundingApplication) {
        var dto = new FundingApplicationDTO();

        dto.setId(fundingApplication.getId());

        if (Objects.nonNull(fundingApplication.getProject())) {
            dto.setProjectId(fundingApplication.getProject().getId());
        }

        if (Objects.nonNull(fundingApplication.getFundingCall())) {
            dto.setFundingCallId(fundingApplication.getFundingCall().getId());
        }

        if (Objects.nonNull(fundingApplication.getRevisedFundingApplication())) {
            dto.setRevisedFundingApplicationId(
                    fundingApplication.getRevisedFundingApplication().getId());
        }

        if (Objects.nonNull(fundingApplication.getFunding())) {
            dto.setFundingId(fundingApplication.getFunding().getId());
        }

        fundingApplication.getOtherFundingSources().forEach(
                fundingPart -> dto.getOtherFundingSources().add(FundingPartConverter.toDTO(fundingPart)));

        dto.setRequestedAmount(new MonetaryAmountDTO());
        if (Objects.nonNull(fundingApplication.getRequestedAmount())) {
            dto.getRequestedAmount().setAmount(
                    fundingApplication.getRequestedAmount().getAmount());
            dto.getRequestedAmount().setCurrencyId(
                    fundingApplication.getRequestedAmount().getCurrency().getId());
        }

        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
                fundingApplication.getDescription()));

        dto.setResponseSummary(MultilingualContentConverter.getMultilingualContentDTO(
                fundingApplication.getResponseSummary()));

        dto.setSubmissionDate(fundingApplication.getSubmissionDate());
        dto.setReviewDateFrom(fundingApplication.getReviewDateFrom());
        dto.setReviewDateTo(fundingApplication.getReviewDateTo());
        dto.setDecisionDate(fundingApplication.getDecisionDate());
        dto.setRevisedProposalOrNextRoundDeadlineDate(
                fundingApplication.getRevisedProposalOrNextRoundDeadlineDate());
        dto.setResult(fundingApplication.getResult());

        fundingApplication.getDocuments().forEach(
                document -> dto.getDocuments().add(DocumentFileConverter.toDTO(document)));

        return dto;
    }
}
