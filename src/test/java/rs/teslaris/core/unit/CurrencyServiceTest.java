package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.project.Currency;
import rs.teslaris.core.repository.project.CurrencyRepository;
import rs.teslaris.core.service.impl.commontypes.CurrencyServiceImpl;

@SpringBootTest
public class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

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
}
