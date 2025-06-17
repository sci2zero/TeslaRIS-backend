package rs.teslaris.importer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.JournalPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalPublicationLoadDTO extends DocumentLoadDTO {

    private JournalPublicationType journalPublicationType;

    private String startPage;

    private String endPage;

    private Integer numberOfPages;

    private String articleNumber;

    private String volume;

    private String issue;

    private String journalEIssn;

    private String journalPrintIssn;

    private String journalOpenAlexId;

    private List<MultilingualContentDTO> journalName;
}
