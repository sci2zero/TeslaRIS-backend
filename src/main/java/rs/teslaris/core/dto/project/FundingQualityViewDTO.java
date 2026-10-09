package rs.teslaris.core.dto.project;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

/**
 * The part of a funding revision the data quality profile has rules for.
 *
 * @see ProjectQualityViewDTO for why the projection exists
 */
@Getter
@Setter
@NoArgsConstructor
public class FundingQualityViewDTO {

    private Integer id;

    private String doi;

    private String grantAgreementId;

    private Integer projectId;

    private Integer involvementId;

    private Set<String> internalIdentifiers = new HashSet<>();

    private Set<String> uris = new HashSet<>();

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private Set<Integer> researchAreasId = new HashSet<>();

    private MonetaryAmountDTO amount;

    private LocalDate dateSubmitted;

    private LocalDate dateAwarded;

    private LocalDate dateFrom;

    private LocalDate dateTo;
}
