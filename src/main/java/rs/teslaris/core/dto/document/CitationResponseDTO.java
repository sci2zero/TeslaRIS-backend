package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitationResponseDTO {

    private String apa;

    private String mla;

    private String chicago;

    private String harvard;

    private String vancouver;
}
