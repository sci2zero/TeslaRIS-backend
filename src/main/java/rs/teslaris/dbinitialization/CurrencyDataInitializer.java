package rs.teslaris.dbinitialization;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.CurrencyRepository;
import rs.teslaris.core.util.search.StringUtil;

/**
 * Seeds the currencies monetary amounts can be expressed in.
 * <p>
 * The order of the {@code saveAll} list below determines the generated IDs.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CurrencyDataInitializer {

    private final CurrencyRepository currencyRepository;


    public void initializeCurrencies(LanguageTag englishTag) {
        var currencyEuro = new Currency();
        currencyEuro.setName(Set.of(new MultiLingualContent(englishTag, "Euro", 1)));
        currencyEuro.setCode("EUR");
        currencyEuro.setSymbol("€");
        currencyEuro.setProcessedName(" " + StringUtil.performSimpleLatinPreprocessing("Euro"));

        var currencyDollar = new Currency();
        currencyDollar.setName(Set.of(new MultiLingualContent(englishTag, "Dollar", 1)));
        currencyDollar.setCode("USD");
        currencyDollar.setSymbol("$");
        currencyDollar.setProcessedName(
            " " + StringUtil.performSimpleLatinPreprocessing("Dollar"));

        var currencySerbianDinar = new Currency();
        currencySerbianDinar.setName(
            Set.of(new MultiLingualContent(englishTag, "Serbian Dinar", 1)));
        currencySerbianDinar.setCode("RSD");
        currencySerbianDinar.setSymbol("RSD");
        currencySerbianDinar.setProcessedName(
            " " + StringUtil.performSimpleLatinPreprocessing("Serbian Dinar"));

        currencyRepository.saveAll(List.of(currencyEuro, currencyDollar, currencySerbianDinar));
    }
}
