package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.service.impl.commontypes.CountryServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
@RequiredArgsConstructor
public class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

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
}
