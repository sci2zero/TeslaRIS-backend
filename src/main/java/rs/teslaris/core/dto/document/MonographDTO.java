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

    private String eisbn;

    private Integer numberOfPages;

    private String volume;

    private String number;

    private Integer publicationSeriesId;

    private List<Integer> languageTagIds;

    private Integer researchAreaId;
}
