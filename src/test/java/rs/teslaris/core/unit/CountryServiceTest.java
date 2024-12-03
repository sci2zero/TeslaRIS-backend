package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.impl.commontypes.CountryServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
@RequiredArgsConstructor
public class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private CountryServiceImpl countryService;


    @Test
    public void shouldReturnCountryWhenCountryExists() {
        // given
        var expectedCountry = new Country();
        expectedCountry.setCode("MNE");

        when(countryRepository.findById(1)).thenReturn(Optional.of(expectedCountry));

        // when
        var actualCountry = countryService.findOne(1);

        // then
        assertEquals(expectedCountry.getCode(), actualCountry.getCode());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenCountryDoesNotExist() {
        // given
        when(countryRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> countryService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReadCountryWhenCountryExists() {
        // given
        var expectedCountry = new Country();
        expectedCountry.setCode("MNE");

        when(countryRepository.findById(1)).thenReturn(Optional.of(expectedCountry));

        // when
        var actualCountry = countryService.readCountryById(1);

        // then
        assertEquals(expectedCountry.getCode(), actualCountry.getCode());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadCountryDoesNotExist() {
        // given
        when(countryRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> countryService.readCountryById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateCountry() {
        // given
        var countryDTO = new CountryDTO();
        countryDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "English Name", 1)));
        countryDTO.setCode("US");

        var country = new Country();
        country.setCode("US");
        when(countryRepository.save(any(Country.class))).thenReturn(country);

        // when
        var result = countryService.createCountry(countryDTO);

        // then
        assertNotNull(result);
        verify(countryRepository).save(any(Country.class));
        assertEquals(countryDTO.getCode(), result.getCode());
    }

    @Test
    public void shouldUpdateCountry() {
        // given
        Integer countryId = 1;
        var countryDTO = new CountryDTO();
        countryDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "English Name", 1)));
        countryDTO.setCode("CA");

        var existingCountry = new Country();
        existingCountry.setCode("US");

        when(countryRepository.findById(countryId)).thenReturn(Optional.of(existingCountry));
        when(multilingualContentService.getMultilingualContent(countryDTO.getName()))
            .thenReturn(Set.of(new MultiLingualContent()));

        // when
        countryService.updateCountry(countryId, countryDTO);

        // then
        verify(countryRepository).save(existingCountry);
        assertEquals("CA", existingCountry.getCode());
    }

    @Test
    public void shouldDeleteCountry() {
        // given
        var countryId = 1;
        when(countryRepository.findById(1)).thenReturn(Optional.of(new Country()));

        // when
        countryService.deleteCountry(countryId);

        // then
        verify(countryRepository).unsetCountryForPersons(countryId);
        verify(countryRepository).findById(countryId);
    }

    @Test
    public void shouldReadAllCountries() {
        // given
        var country = new Country();
        when(countryRepository.findAll()).thenReturn(List.of(country));

        // when
        var result = countryService.readAllCountries();

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(countryRepository).findAll();
    }

    @Test
    public void shouldSearchCountries() {
        // given
        var countryDTO = new CountryDTO();
        var countryPage = new PageImpl<>(List.of(new Country()));
        var pageable = PageRequest.of(0, 10);
        when(countryRepository.searchCountries("Search Term", pageable)).thenReturn(countryPage);

        // when
        var result = countryService.searchCountries(pageable, "Search Term");

        // then
        assertNotNull(result);
        verify(countryRepository).searchCountries("Search Term", pageable);
    }

    @Test
    public void shouldReturnAllCountriesWhenSearchExpressionIsAsterisk() {
        // given
        var countryPage = new PageImpl<>(List.of(new Country()));
        var pageable = PageRequest.of(0, 10);
        when(countryRepository.findAll(pageable)).thenReturn(countryPage);

        // when
        var result = countryService.searchCountries(pageable, "*");

        // then
        assertNotNull(result);
        verify(countryRepository).findAll(pageable);
    }
}
