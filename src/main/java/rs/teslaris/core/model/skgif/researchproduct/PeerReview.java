package rs.teslaris.core.model.skgif.researchproduct;

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
public class PeerReview {

    @JsonProperty("status")
    private String status; // peer reviewed, under review

    @JsonProperty("description")
    private String description;
}
