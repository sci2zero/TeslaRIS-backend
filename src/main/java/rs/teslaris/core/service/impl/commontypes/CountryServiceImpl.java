package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.CountryConverter;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Transactional
@RequiredArgsConstructor
@Traceable
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
    public Page<CountryDTO> searchCountries(Pageable pageable, String searchExpression,
                                            String languageCode) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }

        return countryRepository.searchCountries(
                StringUtil.performSimpleLatinPreprocessing(searchExpression), languageCode, pageable)
            .map(CountryConverter::toDTO);
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
    public Optional<Country> findCountryByName(String name) {
        return countryRepository.findCountryByName(name, Limit.of(1));
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
        country.setName(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                countryDTO.getName()));
        country.setCode(countryDTO.getCode());

        country.setProcessedName("");
        country.getName().forEach(name -> {
            country.setProcessedName(country.getProcessedName() + " " +
                StringUtil.performSimpleLatinPreprocessing(name.getContent()));
        });
    }
}
