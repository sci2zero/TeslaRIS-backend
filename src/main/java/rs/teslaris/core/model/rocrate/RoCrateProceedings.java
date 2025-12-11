package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoCrateProceedings extends RoCratePublicationBase {

    private String additionalType;

    private String conferenceName;


    public RoCrateProceedings() {
        this.setType("Book");
        additionalType = "Proceedings";
    }
}
