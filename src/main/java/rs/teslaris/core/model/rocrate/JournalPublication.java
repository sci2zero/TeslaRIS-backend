package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JournalPublication extends PublicationBase {

    private String periodicalName;

    private String volume;

    private String issue;

    private String pageStart;

    private String pageEnd;

    private String journal;

    private ContextualEntity isPartOf;


    public JournalPublication() {
        this.setType("ScholarlyArticle");
    }
}
