package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.CountryConverter;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@Service
@Transactional
@RequiredArgsConstructor
public class CountryServiceImpl extends JPAServiceImpl<Country> implements CountryService {

    private final CountryRepository countryRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Country, Integer> getEntityRepository() {
        return countryRepository;
    }

    @Override
    public List<CountryDTO> readAllCountries() {
        return countryRepository.findAll().stream().map(CountryConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Page<CountryDTO> searchCountries(Pageable pageable, String searchExpression) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }
        if (searchExpression.isEmpty()) {
            return countryRepository.findAll(pageable).map(CountryConverter::toDTO);
        } else {
            return countryRepository.searchCountries(pageable, searchExpression)
                .map(CountryConverter::toDTO);
        }
    }

    @Override
    public CountryDTO readCountryById(Integer countryId) {
        return CountryConverter.toDTO(this.findOne(countryId));
    }

    @Override
    public CountryDTO createCountry(CountryDTO countryDTO) {
        var newCountry = new Country();

        setCommonFields(newCountry, countryDTO);

        var savedCountry = save(newCountry);

        return CountryConverter.toDTO(savedCountry);
    }

    @Override
    @Nullable
    public Optional<CountryDTO> findCountryByName(String name) {
        return countryRepository.findCountryByName(name);
    }

    @Override
    public void updateCountry(Integer countryId, CountryDTO countryDTO) {
        var countryToUpdate = findOne(countryId);

        setCommonFields(countryToUpdate, countryDTO);

        save(countryToUpdate);
    }

    @Override
    public void deleteCountry(Integer countryId) {
        countryRepository.unsetCountryForPersons(countryId);
        delete(countryId);
    }

    private void setCommonFields(Country country, CountryDTO countryDTO) {
        country.setName(multilingualContentService.getMultilingualContent(countryDTO.getName()));
        country.setCode(countryDTO.getCode());
    }
}
