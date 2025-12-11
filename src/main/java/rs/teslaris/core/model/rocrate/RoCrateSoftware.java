package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoCrateSoftware extends RoCratePublicationBase {

    private String identifier;

    public RoCrateSoftware() {
        this.setType("SoftwareSourceCode");
    }
}
