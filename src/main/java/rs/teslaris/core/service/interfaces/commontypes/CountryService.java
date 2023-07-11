package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Country;

@Service
public interface CountryService {

    Country findCountryById(Integer countryId);
}
