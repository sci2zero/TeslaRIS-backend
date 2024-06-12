package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatentDTO extends DocumentDTO {

    private String number;

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;
}
