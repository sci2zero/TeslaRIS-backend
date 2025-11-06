package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;
import rs.teslaris.core.model.skgif.common.SKGIFTopic;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "product_type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResearchProduct {

    @JsonProperty("local_identifier")
    private String localIdentifier;

    @JsonProperty("identifiers")
    private List<SKGIFIdentifier> identifiers = new ArrayList<>();

    @JsonProperty("entity_type")
    private String entityType = "product";

    @JsonProperty("titles")
    private Map<String, List<String>> titles; // @language container with array values

    @JsonProperty("abstracts")
    private Map<String, List<String>> abstracts; // @language container with array values

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("topics")
    private List<SKGIFTopic> topics;

    @JsonProperty("contributions")
    private List<SKGIFContribution> contributions = new ArrayList<>();

    @JsonProperty("manifestations")
    private List<Manifestation> manifestations = new ArrayList<>();

    @JsonProperty("relevant_organisations")
    private List<String> relevantOrganisations;

    @JsonProperty("funding")
    private List<String> funding;

    @JsonProperty("related_products")
    private RelatedProducts relatedProducts;


    public ResearchProduct(String productType) {
        this.productType = productType;
    }
}
