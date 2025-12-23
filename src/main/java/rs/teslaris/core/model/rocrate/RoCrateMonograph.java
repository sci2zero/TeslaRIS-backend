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
public class RoCrateMonograph extends RoCratePublicationBase {

    private String additionalType = "Book";

    private Integer numberOfPages;

    private String bookEdition;

    private ContextualEntity isPartOf;


    public RoCrateMonograph() {
        this.setType("Book");
    }
}

