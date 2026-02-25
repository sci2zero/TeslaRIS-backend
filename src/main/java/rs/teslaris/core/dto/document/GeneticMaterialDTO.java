package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.GeneticMaterialType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeneticMaterialDTO extends DocumentDTO {

    private String internalNumber;

    @NotNull(message = "You have to provide the genetic material type.")
    private GeneticMaterialType geneticMaterialType;

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    private Boolean authorReprint;
}
