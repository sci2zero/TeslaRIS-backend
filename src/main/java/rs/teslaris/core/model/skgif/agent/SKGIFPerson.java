package rs.teslaris.core.model.skgif.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.teslaris.core.model.skgif.common.SKGIFEntity;

@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SKGIFPerson extends Agent implements SKGIFEntity {

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("affiliations")
    @Builder.Default
    private List<SKGIFAffiliation> affiliations = new ArrayList<>();


    public SKGIFPerson() {
        super("person");
    }
}
