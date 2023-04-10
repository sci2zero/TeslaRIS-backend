package rs.teslaris.core.model.commontypes;


import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class GeoLocation {
    @Column(name = "longitude", nullable = false)
    Double longitude;

    @Column(name = "latitude", nullable = false)
    Double latitude;

    @Column(name = "precision_in_meters", nullable = false)
    int precisionInMeters;
}
