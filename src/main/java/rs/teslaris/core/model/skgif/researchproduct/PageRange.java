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
public class PageRange {

    @JsonProperty("first")
    private String first;

    @JsonProperty("last")
    private String last;
}
