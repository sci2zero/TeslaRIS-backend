package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.JournalPublicationDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedJournalPublicationsDTO extends MergedDocumentsDTO {

    private JournalPublicationDTO leftJournalPublication;

    private JournalPublicationDTO rightJournalPublication;
}
