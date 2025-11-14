package rs.teslaris.core.model.skgif.venue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.teslaris.core.model.skgif.common.SKGIFAccessRights;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;
import rs.teslaris.core.model.skgif.researchproduct.SKGIFContribution;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Venue {

    @JsonProperty("local_identifier")
    protected String localIdentifier;

    @JsonProperty("entity_type")
    protected String entityType = "venue";

    @JsonProperty("type")
    protected String type;

    @JsonProperty("identifiers")
    protected List<SKGIFIdentifier> identifiers = new ArrayList<>();

    @JsonProperty("title")
    protected String title;

    @JsonProperty("acronym")
    protected String acronym;

    @JsonProperty("series")
    protected String series;

    @JsonProperty("creation_date")
    protected String creationDate;

    @JsonProperty("contributions")
    protected List<SKGIFContribution> contributions;

    @JsonProperty("access_rights")
    protected SKGIFAccessRights accessRights;

    public Venue() {
    }

    public Venue(String type) {
        this.type = type;
    }
}
