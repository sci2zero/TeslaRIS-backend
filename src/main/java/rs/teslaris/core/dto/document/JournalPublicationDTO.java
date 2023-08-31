package rs.teslaris.core.dto.document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.JournalPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalPublicationDTO extends DocumentDTO {

    @NotNull(message = "You have to provide a valid publication type.")
    private JournalPublicationType journalPublicationType;

    @NotBlank(message = "You have to provide a start page.")
    private String startPage;

    @NotBlank(message = "You have to provide an end page.")
    private String endPage;

    @NotNull(message = "You have to specify number of pages.")
    @Positive(message = "Number of pages must be a positive number.")
    private Integer numberOfPages;

    @NotBlank(message = "You have to provide an article number.")
    private String articleNumber;

    @NotBlank(message = "You have to provide a volume.")
    private String volume;

    @NotBlank(message = "You have to provide a journal issue.")
    private String issue;

    @NotNull(message = "You have to provide a journal ID.")
    @Positive(message = "PublicationSeries ID must be a positive number.")
    private Integer journalId;
}
