package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.skgif.common.SKGIFAccessRights;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Manifestation {
    @JsonProperty("type")
    private TypeInfo type;

    @JsonProperty("dates")
    private ManifestationDates dates;

    @JsonProperty("identifiers")
    private List<SKGIFIdentifier> identifiers;

    @JsonProperty("peer_review")
    private PeerReview peerReview;

    @JsonProperty("access_rights")
    private SKGIFAccessRights accessRights;

    @JsonProperty("license")
    private String license;

    @JsonProperty("version")
    private String version;

    @JsonProperty("biblio")
    private BibliographicInfo biblio;
}
