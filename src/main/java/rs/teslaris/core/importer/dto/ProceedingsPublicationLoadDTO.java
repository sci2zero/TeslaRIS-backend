package rs.teslaris.core.importer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.ProceedingsPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsPublicationLoadDTO extends DocumentLoadDTO {

    private ProceedingsPublicationType proceedingsPublicationType;

    private String startPage;

    private String endPage;

    private Integer numberOfPages;

    private String articleNumber;

    private List<MultilingualContentDTO> proceedingsName;

    private String confId;

    private List<MultilingualContentDTO> conferenceName;

    private String eIssn;

    private String printIssn;

    private String isbn;

    private String eventDateFrom;

    private String eventDateTo;
}
