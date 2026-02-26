package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
