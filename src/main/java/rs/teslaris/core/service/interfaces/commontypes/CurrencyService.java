package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CurrencyService extends JPAService<Currency> {

    List<CurrencyDTO> getAllCurrencies();

    Page<CurrencyDTO> readCurrencies(Pageable pageable);

    Page<CurrencyDTO> searchCurrencies(Pageable pageable, String searchExpression,
                                       String languageCode);

    CurrencyDTO readCurrencyById(Integer currencyId);

    Currency findCurrencyByCode(String code);

    CurrencyDTO createCurrency(CurrencyDTO currencyDTO);

    void updateCurrency(Integer currencyId, CurrencyDTO currencyDTO);

    void deleteCurrency(Integer currencyId);
}
