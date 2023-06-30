package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsDTO extends DocumentDTO {

    private String eISBN;

    private String printISBN;

    private Integer numberOfPages;

    private String editionTitle;

    private Integer editionNumber;

    private String editionISSN;

    private List<Integer> languageTagIds;

    private Integer journalId;

    private String journalVolume;

    private String journalIssue;

    private Integer eventId;

    private Integer publisherId;
}
