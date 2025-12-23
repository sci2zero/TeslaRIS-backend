package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoCrateJournalPublication extends RoCratePublicationBase {

    private String periodicalName;

    private String volume;

    private String issue;

    private String pageStart;

    private String pageEnd;

    private ContextualEntity isPartOf;


    public RoCrateJournalPublication() {
        this.setType("ScholarlyArticle");
    }
}
