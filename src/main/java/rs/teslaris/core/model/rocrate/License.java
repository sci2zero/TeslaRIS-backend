package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class License extends ContextualEntity {

    private String name;

    private String description;


    public License() {
        this.setType("CreativeWork");
    }
}
