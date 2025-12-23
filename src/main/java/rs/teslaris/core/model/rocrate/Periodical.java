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
public class Periodical extends ContextualEntity implements RoCratePublishable {

    private String name;

    private String issn;

    private ContextualEntity publisher;


    public Periodical() {
        this.setType("Periodical");
    }
}
