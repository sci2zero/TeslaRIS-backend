package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoCratePublicationBase extends ContextualEntity implements RoCratePublishable {

    private String title;

    @JsonProperty("abstract")
    private String abstractText;

    private String publicationYear;

    private String doi;

    private String isbn;

    private String issn;

    private List<ContextualEntity> authors = new ArrayList<>();

    private ContextualEntity publisher;

    private List<ContextualEntity> files = new ArrayList<>();

    private String citeAs;
}
