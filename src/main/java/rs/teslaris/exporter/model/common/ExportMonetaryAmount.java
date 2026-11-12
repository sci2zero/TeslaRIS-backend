package rs.teslaris.exporter.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportMonetaryAmount {

    @Field("amount")
    private double amount;

    @Field("currency_code")
    private String currencyCode;
}
