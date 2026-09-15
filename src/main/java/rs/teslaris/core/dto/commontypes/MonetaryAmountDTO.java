package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.project.model.common.Currency;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonetaryAmountDTO {

    @NotNull(message = "You have to provide currency id.")
    @Positive(message = "Currency id cannot be a negative number.")
    private Integer currencyId;

    @NotNull(message = "You have to provide amount.")
    @Positive(message = "Amount must be a positive number.")
    private double amount;

    private String currencyCode;

    private String currencySymbol;

    public MonetaryAmountDTO(Integer currencyId, double amount) {
        this.currencyId = currencyId;
        this.amount = amount;
    }

    public MonetaryAmountDTO(Currency currency, double amount) {
        this.currencyId = currency.getId();
        this.currencyCode = currency.getCode();
        this.currencySymbol = currency.getSymbol();
        this.amount = amount;
    }

}
