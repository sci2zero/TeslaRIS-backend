package rs.teslaris.core.dto.commontypes;

import javax.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDTO {
    private Double longitude;

    private Double latitude;

    private int precisionInMeters;
}
