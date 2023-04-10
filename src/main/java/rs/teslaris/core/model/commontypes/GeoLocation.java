package rs.teslaris.core.model.commontypes;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation extends BaseEntity {
    Double longitude;
    Double latitude;
    int precisionInMeters;
}
