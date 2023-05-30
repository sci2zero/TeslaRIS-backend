package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.model.commontypes.GeoLocation;

public class GeoLocationDTOToGeoLocation {
    public static GeoLocation fromDTO(GeoLocationDTO geoLocationDTO) {
        var geoLocation = new GeoLocation();
        geoLocation.setLongitude(geoLocationDTO.getLongitude());
        geoLocation.setLatitude(geoLocationDTO.getLatitude());
        geoLocation.setPrecisionInMeters(geoLocationDTO.getPrecisionInMeters());

        return geoLocation;
    }
}
