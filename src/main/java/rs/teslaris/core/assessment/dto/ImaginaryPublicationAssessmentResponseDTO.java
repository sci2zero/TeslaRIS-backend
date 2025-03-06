package rs.teslaris.core.assessment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImaginaryPublicationAssessmentResponseDTO {

    private String assessmentCode;

    private List<MultilingualContentDTO> assessmentReason;

    private Double rawPoints;

    private List<MultilingualContentDTO> rawPointsReason;

    private Double scaledPoints;

    private List<MultilingualContentDTO> scaledPointsReason;
}
