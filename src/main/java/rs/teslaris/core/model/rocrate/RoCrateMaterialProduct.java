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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoCrateMaterialProduct extends RoCratePublicationBase {

    private String identifier;

    private ContextualEntity audience;

    private String category;

    private ContextualEntity brand;

    public RoCrateMaterialProduct() {
        this.setType("Product");
    }
}
