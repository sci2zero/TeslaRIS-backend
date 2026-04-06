package rs.teslaris.core.dto.commontypes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyDTO {

    @Valid
    @NotNull(message = "You have to provide currency name.")
    @NotEmpty(message = "You have to provide currency name.")
    private List<MultilingualContentDTO> currencyName = new ArrayList<>();

    @NotBlank(message = "You have to provide currency symbol.")
    private String symbol;

    @NotBlank(message = "You have to provide currency code.")
    private String code;

    // only for responses
    private Integer currencyId;
}
