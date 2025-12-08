package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Periodical extends ContextualEntity {

    private String name;

    private String issn;

    private String publisher;


    public Periodical() {
        this.setType("Periodical");
    }
}
