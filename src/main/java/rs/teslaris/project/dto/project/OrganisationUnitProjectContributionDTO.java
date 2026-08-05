package rs.teslaris.project.dto.project;

import jakarta.validation.Valid;
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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitProjectContributionDTO {

    private Integer id;

    private Integer organisationUnitId;

    @Valid
    private List<MultilingualContentDTO> displayOrganisationUnit = new ArrayList<>();

    // only for responses
    private List<MultilingualContentDTO> organisationUnitName = new ArrayList<>();

    @Valid
    private List<MultilingualContentDTO> contributionDescription = new ArrayList<>();

    @NotNull(message = "You have to provide a contribution type.")
    private OrganisationUnitProjectContributionType contributionType;

    @NotNull(message = "You have to specify an order number.")
    @Positive(message = "Order number must be a positive number.")
    private Integer orderNumber;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Set<String> uris = new HashSet<>();

    private Boolean isMainContributor;

    private Boolean favorite;

    private Integer contactPersonId;

    @Valid
    private List<FundingPartDTO> fundingParts = new ArrayList<>();

    @Valid
    private List<MultilingualContentDTO> displayProject = new ArrayList<>();

}
