package rs.teslaris.core.model.skgif.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.teslaris.core.model.skgif.common.SKGIFEntity;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SKGIFOrganisation extends Agent implements SKGIFEntity {

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("other_names")
    private List<String> otherNames; // Array of strings, not language map

    @JsonProperty("website")
    private String website;

    @JsonProperty("country")
    private String country;

    @JsonProperty("types")
    private List<String> types; // Array of organization types


    public SKGIFOrganisation() {
        super("organisation");
    }
}
