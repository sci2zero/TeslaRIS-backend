package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoCrateDataset extends RoCratePublicationBase {

    private String identifier;

    private String distribution;


    public RoCrateDataset() {
        this.setType("Dataset");
    }
}
