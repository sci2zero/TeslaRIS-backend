package rs.teslaris.core.model.skgif.researchproduct;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SKGIFContribution {

    @JsonProperty("by")
    private String by;

    @JsonProperty("declared_affiliations")
    private List<String> declaredAffiliations;

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("contribution_types")
    private List<String> contributionTypes;

    @JsonProperty("role")
    private String role;
}
