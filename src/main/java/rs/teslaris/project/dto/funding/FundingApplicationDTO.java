package rs.teslaris.project.dto.funding;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.project.model.funding.FundingApplicationResult;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingApplicationDTO {

    private Integer id;

    private Integer projectId;

    private Integer fundingCallId;

    private Integer revisedFundingApplicationId;

    private Integer fundingId;

    private List<FundingPartDTO> otherFundingSources = new ArrayList<>();

    private MonetaryAmountDTO requestedAmount;

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<MultilingualContentDTO> responseSummary = new ArrayList<>();

    private LocalDate submissionDate;

    private LocalDate reviewDateFrom;

    private LocalDate reviewDateTo;

    private LocalDate decisionDate;

    private LocalDate revisedProposalOrNextRoundDeadlineDate;

    private FundingApplicationResult result;

    private List<DocumentFileResponseDTO> documents = new ArrayList<>();

}
