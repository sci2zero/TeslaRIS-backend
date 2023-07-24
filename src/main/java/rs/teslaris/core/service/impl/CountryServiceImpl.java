package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.CountryService;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl extends JPAServiceImpl<Country> implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    protected JpaRepository<Country, Integer> getEntityRepository() {
        return countryRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Country findCountryById(Integer countryId) {
        return this.findOne(countryId);
    }


}
