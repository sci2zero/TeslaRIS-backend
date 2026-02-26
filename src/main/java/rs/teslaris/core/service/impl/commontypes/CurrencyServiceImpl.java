package rs.teslaris.core.service.impl.commontypes;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.CurrencyConverter;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.model.project.Currency;
import rs.teslaris.core.repository.project.CurrencyRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl extends JPAServiceImpl<Currency> implements CurrencyService {

    private final CurrencyRepository currencyRepository;

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
}
