package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoCratePerson extends ContextualEntity {

    private String givenName;

    private String familyName;

    private String additionalName;

    private String email;

    private String description;

    private List<Organization> affiliations;


    public RoCratePerson() {
        this.setType("Person");
    }
}

