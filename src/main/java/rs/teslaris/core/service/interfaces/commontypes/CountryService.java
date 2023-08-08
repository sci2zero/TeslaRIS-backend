package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.service.JPAService;

@Service
public interface CountryService extends JPAService<Country> {

    Country findCountryById(Integer countryId);
}
