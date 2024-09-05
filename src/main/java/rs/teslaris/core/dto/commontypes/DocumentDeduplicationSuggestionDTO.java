package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDeduplicationSuggestionDTO {

    private Integer id;

    private Integer leftDocumentId;

    private Integer rightDocumentId;

    private List<MultilingualContentDTO> leftDocumentTitle;

    private List<MultilingualContentDTO> rightDocumentTitle;

    private DocumentPublicationType documentPublicationType;
}
