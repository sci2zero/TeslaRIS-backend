package rs.teslaris.core.dto.document;

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

    private JournalPublicationType journalPublicationType;

    private String startPage;

    private String endPage;

    @Positive(message = "Number of pages must be a positive number.")
    private Integer numberOfPages;

    private String articleNumber;

    private String volume;

    private String issue;

    @NotNull(message = "You have to provide a journal ID.")
    @Positive(message = "Journal ID must be a positive number.")
    private Integer journalId;
}
