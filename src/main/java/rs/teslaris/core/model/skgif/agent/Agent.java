package rs.teslaris.core.model.skgif.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "entity_type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SKGIFPerson.class, name = "person"),
    @JsonSubTypes.Type(value = SKGIFOrganisation.class, name = "organisation")
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class Agent {

    @JsonProperty("local_identifier")
    protected String localIdentifier;

    @JsonProperty("entity_type")
    protected String entityType;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("identifiers")
    @Builder.Default
    protected List<SKGIFIdentifier> identifiers = new ArrayList<>();


    public Agent(String entityType) {
        this.entityType = entityType;
    }
}
