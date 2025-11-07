package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
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
public class TypeInfo {

    @JsonProperty("class")
    private String clazz;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("defined_in")
    private String definedIn;
}