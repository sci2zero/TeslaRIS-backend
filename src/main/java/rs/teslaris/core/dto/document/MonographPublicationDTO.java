package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.MonographPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonographPublicationDTO extends DocumentDTO {

    private MonographPublicationType monographPublicationType;

    private String startPage;

    private String endPage;

    private Integer numberOfPages;

    private String articleNumber;

    private Integer monographId;
}
