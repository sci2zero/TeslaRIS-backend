package rs.teslaris.exporter.model.skgif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SKGIFSingleResponse {

    @JsonProperty("@context")
    private List<Object> context;

    @JsonProperty("@graph")
    private List<Object> graph;


    public SKGIFSingleResponse(List<Object> entities, String baseUrl) {
        this.context = Arrays.asList(
            "https://w3id.org/skg-if/context/skg-if.json",
            Map.of("@base", baseUrl + "/api/skg-if")
        );

        this.graph = new ArrayList<>(entities);
    }
}
