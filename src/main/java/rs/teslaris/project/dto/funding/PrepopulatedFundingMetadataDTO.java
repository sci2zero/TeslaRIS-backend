package rs.teslaris.project.dto.funding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedFundingMetadataDTO {

    private String doi;

    private String grantAgreementId;

    private List<MultilingualContentDTO> name = new ArrayList<>();

    private List<MultilingualContentDTO> nameAbbreviation = new ArrayList<>();

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<String> uris = new ArrayList<>();

    private String dateAwarded;

    private String dateFrom;

    private String dateTo;

    private MonetaryAmountDTO monetaryAmount;

    private List<MultilingualContentDTO> displayFunder = new ArrayList<>();

    private String funderDoi;
}