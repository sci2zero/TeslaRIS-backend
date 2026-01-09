package rs.teslaris.exporter.model.skgif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"@context", "@graph"})
public class SKGIFSingleResponse<T> {

    @JsonProperty("@context")
    private List<Object> context;

    @JsonProperty("@graph")
    private List<T> graph;


    public SKGIFSingleResponse(List<T> entities, String baseUrl) {
        this.context = Arrays.asList(
            "https://w3id.org/skg-if/context/skg-if.json",
            "https://w3id.org/skg-if/context/1.0.0/skg-if-api.json",
            Map.of("@base", baseUrl + "/api/skg-if")
        );

        this.graph = new ArrayList<T>(entities);
    }
}
