package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalDTO {

    private Integer id;

    private List<MultilingualContentDTO> title;

    private String eISSN;

    private String printISSN;

    private List<PersonJournalContributionDTO> contributions;

    private List<Integer> languageTagIds;

    private List<MultilingualContentDTO> nameAbbreviation;
}
