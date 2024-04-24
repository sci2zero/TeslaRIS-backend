package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CountryService extends JPAService<Country> {

    Country findCountryById(Integer countryId);

    List<CountryDTO> readAllCountries();

    CountryDTO readCountryById(Integer countryId);
}
