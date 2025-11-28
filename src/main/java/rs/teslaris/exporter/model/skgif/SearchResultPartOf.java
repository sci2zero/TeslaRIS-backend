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
public class SearchResultPartOf {

    @JsonProperty("local_identifier")
    private String localIdentifier;

    @JsonProperty("entity_type")
    private String entityType = "search_result";

    @JsonProperty("total_items")
    private long totalItems;

    @JsonProperty("first_page")
    private SearchResultPage firstPage;

    @JsonProperty("last_page")
    private SearchResultPage lastPage;
}
