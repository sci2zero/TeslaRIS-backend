package rs.teslaris.project.dto.funding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.model.funding.FundingType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingDTO {

    private String internalIdentifier;

    private String doi;

    private String grantAgreementId;

    @NotNull(message = "You have to provide project ID.")
    @Positive(message = "Project ID cannot be a negative number.")
    private Integer projectId;

    @Valid
    @NotNull(message = "You have to provide a funding name.")
    @NotEmpty(message = "You have to provide a funding name.")
    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<MultilingualContentDTO> objectives = new ArrayList<>();

    private List<MultilingualContentDTO> nameAbbreviation = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private Set<Integer> researchAreasId = new HashSet<>();

    private Set<String> uris = new HashSet<>();

    private Integer fundingCallId;

    private List<MultilingualContentDTO> displayCall = new ArrayList<>();

    private List<MultilingualContentDTO> displayProgram = new ArrayList<>();

    private List<MultilingualContentDTO> displayFunder = new ArrayList<>();

    @NotNull(message = "You have to provide funding types.")
    private Set<FundingType> fundingTypes = new HashSet<>();

    private MonetaryAmountDTO amount;

    private Boolean competitive;

    private Boolean renewable;

    private LocalDate dateSubmitted;

    private LocalDate dateAwarded;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Boolean oaMandated;

    private String oaMandateUrl;
}
