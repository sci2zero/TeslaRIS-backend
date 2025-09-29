package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentIdentifierUpdateDTO {
    private String doi;

    private String scopusId;

    private String openAlexId;

    private String webOfScienceId;
}
