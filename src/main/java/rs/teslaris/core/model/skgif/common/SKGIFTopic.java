package rs.teslaris.core.model.skgif.common;

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
public class SKGIFTopic {

    @JsonProperty("term")
    private String term;

    @JsonProperty("provenance")
    private List<Provenance> provenance;
}
