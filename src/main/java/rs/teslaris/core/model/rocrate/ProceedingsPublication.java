package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProceedingsPublication extends PublicationBase {

    private String inProceedingsTitle;

    private ContextualEntity proceedings;


    public ProceedingsPublication() {
        this.setType("ScholarlyArticle");
    }
}
