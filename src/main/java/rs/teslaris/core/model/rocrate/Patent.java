package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Patent extends PublicationBase {

    private String patentNumber;


    public Patent() {
        this.setType("Patent");
    }
}
