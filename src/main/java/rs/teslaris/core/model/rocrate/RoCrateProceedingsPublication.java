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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoCrateProceedingsPublication extends RoCratePublicationBase {

    private String inProceedingsTitle;

    private ContextualEntity proceedings;


    public RoCrateProceedingsPublication() {
        this.setType("ScholarlyArticle");
    }
}
