package rs.teslaris.core.model.skgif.agent;

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
public class SKGIFAffiliation {

    @JsonProperty("affiliation")
    private String affiliation;

    @JsonProperty("role")
    private String role;

    @JsonProperty("period")
    private TimePeriod period;
}
