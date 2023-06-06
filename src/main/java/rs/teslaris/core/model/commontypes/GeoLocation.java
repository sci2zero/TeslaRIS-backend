package rs.teslaris.core.model.commontypes;


import java.util.Objects;
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
    private Double longitude;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "precision_in_meters", nullable = false)
    private int precisionInMeters;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeoLocation that = (GeoLocation) o;
        return precisionInMeters == that.precisionInMeters && longitude.equals(that.longitude) &&
            latitude.equals(that.latitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longitude, latitude, precisionInMeters);
    }
}
