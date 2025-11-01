package rs.teslaris.core.model.skgif.common;

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
public class SKGIFAccessRights {

    @JsonProperty("status")
    private String status; // open, closed, embargoed, restricted, unavailable

    @JsonProperty("description")
    private String description;
}
