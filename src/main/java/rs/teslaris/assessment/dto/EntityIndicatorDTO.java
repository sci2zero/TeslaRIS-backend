package rs.teslaris.assessment.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityIndicatorDTO {

    private Double numericValue;

    private Boolean booleanValue;

    private String textualValue;

    private LocalDate fromDate;

    private LocalDate toDate;

    @NotNull(message = "You have to provide indicator ID.")
    @Positive(message = "Indicator ID must be a positive number.")
    private Integer indicatorId;
}
