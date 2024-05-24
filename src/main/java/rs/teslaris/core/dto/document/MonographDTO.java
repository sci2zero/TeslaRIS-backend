package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.MonographType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonographDTO extends DocumentDTO {

    private Integer id;

    private MonographType monographType;

    private String printISBN;

    private String eISBN;

    private Integer numberOfPages;

    private String volume;

    private Integer number;

    private Integer bookSeriesId;

    private Integer journalId;

    private List<Integer> languageTagIds;

    private Integer researchAreaId;
}
