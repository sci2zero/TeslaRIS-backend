package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Proceedings extends PublicationBase {

    private String additionalType;

    private String conferenceName;


    public Proceedings() {
        this.setType("Book");
        additionalType = "Proceedings";
    }
}
