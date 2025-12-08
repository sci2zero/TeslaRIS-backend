package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicationBase extends ContextualEntity {

    private String title;

    @JsonProperty("abstract")
    private String abstractText;

    private String publicationYear;

    private String doi;

    private String isbn;

    private String issn;

    private List<ContextualEntity> authors;

    private List<ContextualEntity> editors;

    private ContextualEntity publisher;

    private List<ContextualEntity> files;

    private String citeAs;
}
