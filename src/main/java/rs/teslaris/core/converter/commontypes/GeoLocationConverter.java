package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.model.commontypes.GeoLocation;

public class GeoLocationConverter {
    public static GeoLocation fromDTO(GeoLocationDTO geoLocationDTO) {
        var geoLocation = new GeoLocation();
        geoLocation.setLongitude(geoLocationDTO.getLongitude());
        geoLocation.setLatitude(geoLocationDTO.getLatitude());
        geoLocation.setAddress(geoLocationDTO.getAddress());

        return geoLocation;
    }

    public static GeoLocationDTO toDTO(GeoLocation geoLocation) {
        var geoLocationDTO = new GeoLocationDTO();
        if (geoLocation != null) {
            geoLocationDTO.setLongitude(geoLocation.getLongitude());
            geoLocationDTO.setLatitude(geoLocation.getLatitude());
            geoLocationDTO.setAddress(geoLocation.getAddress());
        }
        return geoLocationDTO;
    }
}
