package rs.teslaris.exporter.model.skgif;

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
public class SKGIFMeta {

    @JsonProperty("local_identifier")
    private String localIdentifier;

    @JsonProperty("entity_type")
    private String entityType = "search_result_page";

    @JsonProperty("next_page")
    private SearchResultPage nextPage;

    @JsonProperty("prev_page")
    private SearchResultPage prevPage;

    @JsonProperty("part_of")
    private SearchResultPartOf partOf;
}
