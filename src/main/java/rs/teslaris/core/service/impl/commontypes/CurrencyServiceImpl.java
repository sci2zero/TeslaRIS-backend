package rs.teslaris.core.service.impl.commontypes;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.CurrencyConverter;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.repository.commontypes.CurrencyRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyUsageChecker;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Transactional
@RequiredArgsConstructor
@Traceable
public class CurrencyServiceImpl extends JPAServiceImpl<Currency> implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    private final MultilingualContentService multilingualContentService;

    private final CurrencyUsageChecker currencyUsageChecker;


    @Override
    protected JpaRepository<Currency, Integer> getEntityRepository() {
        return currencyRepository;
    }

    @Override
    public List<CurrencyDTO> getAllCurrencies() {
        return findAll().stream().map(CurrencyConverter::toDTO).toList();
    }

    @Override
    public Page<CurrencyDTO> readCurrencies(Pageable pageable) {
        return findAll(pageable).map(CurrencyConverter::toDTO);
    }

    @Override
    public Page<CurrencyDTO> searchCurrencies(Pageable pageable, String searchExpression,
                                              String languageCode) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }

        return currencyRepository.searchCurrencies(
                StringUtil.performSimpleLatinPreprocessing(searchExpression), languageCode, pageable)
            .map(CurrencyConverter::toDTO);
    }

    @Override
    public CurrencyDTO readCurrencyById(Integer currencyId) {
        return CurrencyConverter.toDTO(findOne(currencyId));
    }

    @Override
    public Currency findCurrencyByCode(String code) {
        return currencyRepository.findByCode(code).orElse(null);
    }

    @Override
    public CurrencyDTO createCurrency(CurrencyDTO currencyDTO) {
        var newCurrency = new Currency();

        setCommonFields(newCurrency, currencyDTO);

        return CurrencyConverter.toDTO(save(newCurrency));
    }

    @Override
    public void updateCurrency(Integer currencyId, CurrencyDTO currencyDTO) {
        var currencyToUpdate = findOne(currencyId);

        setCommonFields(currencyToUpdate, currencyDTO);

        save(currencyToUpdate);
    }

    @Override
    public void deleteCurrency(Integer currencyId) {
        if (currencyUsageChecker.isCurrencyInUse(currencyId)) {
            throw new ReferenceConstraintException("currencyInUseMessage");
        }

        delete(currencyId);
    }

    private void setCommonFields(Currency currency, CurrencyDTO currencyDTO) {
        currency.setName(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                currencyDTO.getCurrencyName()));
        currency.setSymbol(currencyDTO.getSymbol());
        currency.setCode(currencyDTO.getCode());

        currency.setProcessedName("");
        currency.getName().forEach(name -> {
            currency.setProcessedName(currency.getProcessedName() + " " +
                StringUtil.performSimpleLatinPreprocessing(name.getContent()));
        });
    }
}
