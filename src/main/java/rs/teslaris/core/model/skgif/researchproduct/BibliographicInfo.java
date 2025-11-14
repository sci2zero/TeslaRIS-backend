package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BibliographicInfo {

    @JsonProperty("issue")
    private String issue;

    @JsonProperty("pages")
    private PageRange pages;

    @JsonProperty("volume")
    private String volume;

    @JsonProperty("in")
    private String in;

    @JsonProperty("hosting_data_source")
    private String hostingDataSource;
}
