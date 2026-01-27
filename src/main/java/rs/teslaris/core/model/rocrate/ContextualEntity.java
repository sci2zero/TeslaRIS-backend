package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ContextualEntity {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    private String name;

    private ContextualEntity creator;

    private List<ContextualEntity> hasPart;

    private ContextualEntity conformsTo;

    private ContextualEntity about;


    public ContextualEntity(String id) {
        this.id = id;
    }

    public ContextualEntity(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public ContextualEntity(String id, String type, ContextualEntity conformsTo,
                            ContextualEntity about) {
        this.id = id;
        this.type = type;
        this.conformsTo = conformsTo;
        this.about = about;
    }
}
