package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.CurrencyRepository;
import rs.teslaris.core.service.impl.commontypes.CurrencyServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyUsageChecker;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
public class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CurrencyUsageChecker currencyUsageChecker;

    @InjectMocks
    private CurrencyServiceImpl currencyService;


    @Test
    public void shouldReturnAllCurrenciesAsDTOs() {
        // given
        var currency1 = new Currency();
        currency1.setId(1);
        currency1.setCode("USD");
        currency1.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(new LanguageTag(), "US Dollar", 1)
        })));

        var currency2 = new Currency();
        currency2.setId(2);
        currency2.setCode("EUR");
        currency2.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(new LanguageTag(), "Euro", 1)
        })));

        var currencies = List.of(currency1, currency2);

        when(currencyRepository.findAll()).thenReturn(currencies);

        // when
        var result = currencyService.getAllCurrencies();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.getFirst().getCurrencyId());
        assertEquals("USD", result.getFirst().getCode());
        assertEquals("US Dollar", result.getFirst().getCurrencyName().getFirst().getContent());
        assertEquals(2, result.get(1).getCurrencyId());
        assertEquals("EUR", result.get(1).getCode());
        assertEquals("Euro", result.get(1).getCurrencyName().getFirst().getContent());

        verify(currencyRepository).findAll();
    }

    @Test
    public void shouldReturnEmptyListWhenNoCurrenciesExist() {
        // given
        when(currencyRepository.findAll()).thenReturn(List.of());

        // when
        var result = currencyService.getAllCurrencies();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(currencyRepository).findAll();
    }

    @Test
    public void shouldReturnPagedCurrenciesAsDTOs() {
        // given
        var pageable = PageRequest.of(0, 10);

        var currency1 = new Currency();
        currency1.setId(1);
        currency1.setCode("USD");
        currency1.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(new LanguageTag(), "US Dollar", 1)
        })));

        var currency2 = new Currency();
        currency2.setId(2);
        currency2.setCode("EUR");
        currency2.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(new LanguageTag(), "Euro", 1)
        })));

        var currencyPage = new PageImpl<>(List.of(currency1, currency2), pageable, 2);

        when(currencyRepository.findAll(pageable)).thenReturn(currencyPage);

        // when
        var result = currencyService.readCurrencies(pageable);

        // then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getContent().getFirst().getCurrencyId());
        assertEquals("USD", result.getContent().getFirst().getCode());
        assertEquals("US Dollar",
            result.getContent().getFirst().getCurrencyName().getFirst().getContent());
        assertEquals(2, result.getContent().get(1).getCurrencyId());
        assertEquals("EUR", result.getContent().get(1).getCode());
        assertEquals("Euro", result.getContent().get(1).getCurrencyName().getFirst().getContent());

        verify(currencyRepository).findAll(pageable);
    }

    @Test
    public void shouldReturnEmptyPageWhenNoCurrenciesExist() {
        // given
        var pageable = PageRequest.of(0, 10);
        var emptyPage = new PageImpl<Currency>(List.of(), pageable, 0);

        when(currencyRepository.findAll(pageable)).thenReturn(emptyPage);

        // when
        var result = currencyService.readCurrencies(pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(currencyRepository).findAll(pageable);
    }

    @Test
    public void shouldReadCurrencyWhenCurrencyExists() {
        // given
        var currency = new Currency();
        currency.setCode("EUR");

        when(currencyRepository.findById(1)).thenReturn(Optional.of(currency));

        // when
        var result = currencyService.readCurrencyById(1);

        // then
        assertEquals("EUR", result.getCode());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadCurrencyDoesNotExist() {
        // given
        when(currencyRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> currencyService.readCurrencyById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateCurrency() {
        // given
        var currencyDTO = new CurrencyDTO();
        currencyDTO.setCurrencyName(List.of(new MultilingualContentDTO(1, "EN", "Euro", 1)));
        currencyDTO.setSymbol("€");
        currencyDTO.setCode("EUR");

        var currency = new Currency();
        currency.setCode("EUR");
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);

        // when
        var result = currencyService.createCurrency(currencyDTO);

        // then
        assertNotNull(result);
        verify(currencyRepository).save(any(Currency.class));
        assertEquals(currencyDTO.getCode(), result.getCode());
    }

    @Test
    public void shouldUpdateCurrencyAndSetProcessedName() {
        // given
        var currencyDTO = new CurrencyDTO();
        currencyDTO.setCurrencyName(List.of(new MultilingualContentDTO(1, "EN", "Euro", 1)));
        currencyDTO.setSymbol("€");
        currencyDTO.setCode("EUR");

        var existingCurrency = new Currency();
        existingCurrency.setCode("USD");

        when(currencyRepository.findById(1)).thenReturn(Optional.of(existingCurrency));
        when(multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
            currencyDTO.getCurrencyName())).thenReturn(
            Set.of(new MultiLingualContent(new LanguageTag(), "Euro", 1)));

        // when
        currencyService.updateCurrency(1, currencyDTO);

        // then
        verify(currencyRepository).save(existingCurrency);
        assertEquals("EUR", existingCurrency.getCode());
        assertEquals("€", existingCurrency.getSymbol());
        assertEquals(" euro", existingCurrency.getProcessedName());
    }

    @Test
    public void shouldDeleteCurrencyWhenNotInUse() {
        // given
        var currency = new Currency();
        when(currencyUsageChecker.isCurrencyInUse(1)).thenReturn(false);
        when(currencyRepository.findById(1)).thenReturn(Optional.of(currency));

        // when
        currencyService.deleteCurrency(1);

        // then
        assertTrue(currency.getDeleted());
        verify(currencyRepository).save(currency);
    }

    @Test
    public void shouldThrowReferenceConstraintExceptionWhenDeletingCurrencyInUse() {
        // given
        when(currencyUsageChecker.isCurrencyInUse(1)).thenReturn(true);

        // when
        assertThrows(ReferenceConstraintException.class,
            () -> currencyService.deleteCurrency(1));

        // then
        verify(currencyRepository, never()).save(any(Currency.class));
    }

    @Test
    public void shouldSearchCurrencies() {
        // given
        var searchTerm = "Search Term";
        var pageable = PageRequest.of(0, 10);
        when(currencyRepository.searchCurrencies(searchTerm.toLowerCase(), "EN", pageable))
            .thenReturn(new PageImpl<>(List.of(new Currency())));

        // when
        var result = currencyService.searchCurrencies(pageable, searchTerm, "EN");

        // then
        assertNotNull(result);
        verify(currencyRepository).searchCurrencies(searchTerm.toLowerCase(), "EN", pageable);
    }

    @Test
    public void shouldReturnAllCurrenciesWhenSearchExpressionIsAsterisk() {
        // given
        var pageable = PageRequest.of(0, 10);
        when(currencyRepository.searchCurrencies("", LanguageAbbreviations.SERBIAN, pageable))
            .thenReturn(new PageImpl<>(List.of(new Currency())));

        // when
        var result =
            currencyService.searchCurrencies(pageable, "*", LanguageAbbreviations.SERBIAN);

        // then
        assertNotNull(result);
        verify(currencyRepository).searchCurrencies("", LanguageAbbreviations.SERBIAN, pageable);
    }
}
