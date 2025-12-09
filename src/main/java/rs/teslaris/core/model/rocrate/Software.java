package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Software extends PublicationBase {

    private String identifier;

    public Software() {
        this.setType("SoftwareSourceCode");
    }
}
