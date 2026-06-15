package rs.teslaris.core.dto.document;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.EventContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonEventContributionDTO extends PersonContributionDTO {

    private EventContributionType eventContributionType;

    private String lectureHoursPerWeek;

    private String tutorialHoursPerWeek;

    private String labHoursPerWeek;

    private String otherContactHoursPerWeek;

    private Integer numberOfReviewsOrAssessment;

    private List<MultilingualContentDTO> displayEvent = new ArrayList<>();

    private List<MultilingualContentDTO> caseName = new ArrayList<>();

    private List<MultilingualContentDTO> locationJurisdiction = new ArrayList<>();

    private Boolean mainArguer;
}
