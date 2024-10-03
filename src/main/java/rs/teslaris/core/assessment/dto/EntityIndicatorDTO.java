package rs.teslaris.core.assessment.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
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
public class EntityIndicatorDTO {

    private Double numericValue;

    private Boolean booleanValue;

    private String textualValue;

    @NotNull(message = "You have to provide a start date.")
    private LocalDate fromDate;

    @NotNull(message = "You have to provide an end date.")
    private LocalDate toDate;

    private List<String> urls = new ArrayList<>();

    @NotNull(message = "You have to provide indicator ID.")
    @Positive(message = "Indicator ID must be a positive number.")
    private Integer indicatorId;
}
