package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextualEntity {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    private ContextualEntity conformsTo;

    private ContextualEntity about;


    public ContextualEntity(String id) {
        this.id = id;
    }

    public ContextualEntity(String id, String type) {
        this.id = id;
        this.type = type;
    }
}
