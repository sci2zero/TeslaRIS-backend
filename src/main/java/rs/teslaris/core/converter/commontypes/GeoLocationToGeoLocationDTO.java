package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.model.commontypes.GeoLocation;

public class GeoLocationToGeoLocationDTO {
    public static GeoLocationDTO toDTO(GeoLocation geoLocation) {
        var geoLocationDTO = new GeoLocationDTO();
        geoLocationDTO.setLongitude(geoLocation.getLongitude());
        geoLocationDTO.setLatitude(geoLocation.getLatitude());
        geoLocationDTO.setPrecisionInMeters(geoLocation.getPrecisionInMeters());

        return geoLocationDTO;
    }
}
