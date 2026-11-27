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
 * The order of the {@code saveAll} list below determines the generated IDs, so new currencies go
 * at the end.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CurrencyDataInitializer {

    private final CurrencyRepository currencyRepository;


    public void initializeCurrencies(LanguageTag englishTag) {
        currencyRepository.saveAll(List.of(
            currency(englishTag, "Euro", "EUR", "€"),
            currency(englishTag, "Dollar", "USD", "$"),
            currency(englishTag, "Serbian Dinar", "RSD", "RSD"),
            // Found in migrated curricula
            currency(englishTag, "Brazilian Real", "BRL", "R$"),
            currency(englishTag, "Pound Sterling", "GBP", "£"),
            currency(englishTag, "Japanese Yen", "JPY", "¥"),
            currency(englishTag, "Swedish Krona", "SEK", "kr"),
            currency(englishTag, "Colombian Peso", "COP", "COP"),
            currency(englishTag, "Cuban Peso", "CUP", "CUP")
        ));
    }

    private Currency currency(LanguageTag englishTag, String name, String code, String symbol) {
        var currency = new Currency();
        currency.setName(Set.of(new MultiLingualContent(englishTag, name, 1)));
        currency.setCode(code);
        currency.setSymbol(symbol);
        currency.setProcessedName(" " + StringUtil.performSimpleLatinPreprocessing(name));
        return currency;
    }
}
