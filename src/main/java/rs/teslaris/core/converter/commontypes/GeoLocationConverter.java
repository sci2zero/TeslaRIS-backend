package rs.teslaris.core.converter.commontypes;

import java.util.Objects;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.model.commontypes.GeoLocation;

public class GeoLocationConverter {
    public static GeoLocation fromDTO(GeoLocationDTO geoLocationDTO) {
        var geoLocation = new GeoLocation();
        geoLocation.setLongitude(
            (Objects.nonNull(geoLocation.getLongitude()) && geoLocationDTO.getLongitude() != 0) ?
                geoLocation.getLongitude() : null);
        geoLocation.setLatitude(
            (Objects.nonNull(geoLocation.getLatitude()) && geoLocationDTO.getLatitude() != 0) ?
                geoLocation.getLatitude() : null);

        if (Objects.nonNull(geoLocationDTO.getAddress())) {
            if (geoLocationDTO.getAddress().isBlank() ||
                geoLocationDTO.getAddress().toLowerCase().contains("undefined")) {
                geoLocation.setAddress(null);
            } else {
                geoLocation.setAddress(geoLocationDTO.getAddress());
            }
        }

        return geoLocation;
    }

    public static GeoLocationDTO toDTO(GeoLocation geoLocation) {
        var geoLocationDTO = new GeoLocationDTO();

        if (Objects.nonNull(geoLocation)) {
            geoLocationDTO.setLongitude(geoLocation.getLongitude());
            geoLocationDTO.setLatitude(geoLocation.getLatitude());
            geoLocationDTO.setAddress(geoLocation.getAddress());
        }

        return geoLocationDTO;
    }
}
