package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoCratePatent extends RoCratePublicationBase {

    private String patentNumber;


    public RoCratePatent() {
        this.setType("Patent");
    }
}
