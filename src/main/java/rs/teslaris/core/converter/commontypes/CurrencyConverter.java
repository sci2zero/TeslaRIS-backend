package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.model.project.Currency;

public class CurrencyConverter {

    public static CurrencyDTO toDTO(Currency currency) {
        var dto = new CurrencyDTO();

        dto.setCurrencyId(currency.getId());
        dto.setCode(currency.getCode());
        dto.setSymbol(currency.getSymbol());
        dto.setCurrencyName(
            MultilingualContentConverter.getMultilingualContentDTO(currency.getName()));

        return dto;
    }
}
