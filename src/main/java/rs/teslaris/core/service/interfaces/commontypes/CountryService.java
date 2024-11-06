package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CountryService extends JPAService<Country> {

    List<CountryDTO> readAllCountries();

    Page<CountryDTO> searchCountries(Pageable pageable, String searchExpression);

    CountryDTO readCountryById(Integer countryId);

    CountryDTO createCountry(CountryDTO countryDTO);

    Optional<Country> findCountryByName(String name);

    void updateCountry(Integer countryId, CountryDTO countryDTO);

    void deleteCountry(Integer countryId);
}
