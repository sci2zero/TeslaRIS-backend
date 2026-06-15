package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.ArticleCollectionSeriesType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponseDTO extends PublicationSeriesDTO {

    private List<String> languageTagNames;

    private ArticleCollectionSeriesType type;
}
