package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.CountryService;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl extends JPAServiceImpl<Country> implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    protected JPASoftDeleteRepository<Country> getEntityRepository() {
        return countryRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Country findCountryById(Integer countryId) {
        return this.findOne(countryId);
    }


}
