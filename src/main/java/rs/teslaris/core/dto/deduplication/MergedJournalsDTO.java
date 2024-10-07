package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.JournalDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedJournalsDTO {

    private JournalDTO leftJournal;

    private JournalDTO rightJournal;
}
