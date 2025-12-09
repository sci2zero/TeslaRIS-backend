package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoCrate {

    @JsonProperty("@context")
    private String context = "https://w3id.org/ro/crate/1.1/context";

    @JsonProperty("@graph")
    private List<Object> graph = new ArrayList<>();
}
