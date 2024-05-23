package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.ProceedingsPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsPublicationDTO extends DocumentDTO {

    @NotNull(message = "You have to provide a valid publication type.")
    private ProceedingsPublicationType proceedingsPublicationType;

    @NotNull(message = "You have to provide a start page.")
    private String startPage;

    @NotNull(message = "You have to provide an end page.")
    private String endPage;

    private Integer numberOfPages;

    @NotNull(message = "You have to provide an article number.")
    private String articleNumber;

    @NotNull(message = "You have to provide a proceedings ID.")
    @Positive(message = "Proceedings ID must be a positive number.")
    private Integer proceedingsId;
}
