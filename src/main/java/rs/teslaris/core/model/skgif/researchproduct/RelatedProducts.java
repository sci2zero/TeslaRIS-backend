package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
public class RelatedProducts {

    @JsonProperty("cites")
    private List<String> cites;

    @JsonProperty("is_supplemented_by")
    private List<String> isSupplementedBy;

    @JsonProperty("is_documented_by")
    private List<String> isDocumentedBy;

    @JsonProperty("is_new_version_of")
    private List<String> isNewVersionOf;

    @JsonProperty("is_part_of")
    private List<String> isPartOf;
}
