package rs.teslaris.assessment.dto;

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


    public void setRawPoints(Double rawPoints) {
        this.rawPoints = roundPrecision(rawPoints);
    }

    public void setScaledPoints(Double scaledPoints) {
        this.scaledPoints = roundPrecision(scaledPoints);
    }

    private double roundPrecision(double value) {
        return Math.floor(value * 100) / 100;
    }
}
