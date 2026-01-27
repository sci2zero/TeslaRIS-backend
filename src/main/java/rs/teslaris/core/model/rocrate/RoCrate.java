package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoCrate {

    @JsonProperty("@context")
    private String context = "https://w3id.org/ro/crate/1.1/context";

    @JsonProperty("@graph")
    private Set<Object> graph = new LinkedHashSet<>();


    public void reverseGraph() {
        var list = new ArrayList<>(this.graph);
        Collections.reverse(list);
        this.graph = new LinkedHashSet<>(list);
    }
}
