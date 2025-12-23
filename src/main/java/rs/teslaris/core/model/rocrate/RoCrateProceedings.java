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
public class RoCrateProceedings extends RoCratePublicationBase {

    private String additionalType;

    private String conferenceName;


    public RoCrateProceedings() {
        this.setType("Book");
        additionalType = "Proceedings";
    }
}
