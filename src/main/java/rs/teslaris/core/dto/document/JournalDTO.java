package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.core.model.document.ArticleCollectionSeriesType;

@Getter
@Setter
public class JournalDTO extends PublicationSeriesDTO {

    @NotNull(message = "You have to provide article collection series type.")
    private ArticleCollectionSeriesType type;
}
