package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    public Country findCountryById(Integer countryId) {
        return countryRepository.findById(countryId)
            .orElseThrow(() -> new NotFoundException("Country with given ID does not exist."));
    }
}
