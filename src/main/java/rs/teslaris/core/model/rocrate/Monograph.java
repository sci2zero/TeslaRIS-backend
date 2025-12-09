package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Monograph extends PublicationBase {

    private String additionalType = "Book";

    private Integer numberOfPages;

    private String bookEdition;


    public Monograph() {
        this.setType("Book");
    }
}

