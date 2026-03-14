package rs.teslaris.core.dto.project;

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
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.project.FundingType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingCallDTO {

    @Valid
    @NotNull(message = "You have to provide a program name.")
    @NotEmpty(message = "You have to provide a program name.")
    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<MultilingualContentDTO> objectives = new ArrayList<>();

    private List<MultilingualContentDTO> nameAbbreviation = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private Set<Integer> researchAreasId = new HashSet<>();

    @NotNull(message = "You have to provide funder program ID.")
    @Positive(message = "Funder program ID cannot be a negative number.")
    private Integer fundingProgramId;

    @NotNull(message = "You have to provide funding types.")
    private Set<FundingType> fundingTypes = new HashSet<>();

    private MonetaryAmountDTO monetaryAmount;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Set<String> uris = new HashSet<>();

    private Boolean oaMandated;

    private String oaMandateUrl;

    // Used only for responses
    private Integer id;

    private List<DocumentFileResponseDTO> fileItems = new ArrayList<>();

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();

    private List<MultilingualContentDTO> fundingProgramName = new ArrayList<>();
}
