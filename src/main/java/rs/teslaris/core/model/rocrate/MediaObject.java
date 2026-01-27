package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MediaObject {

    @JsonProperty("@type")
    private final String type = "MediaObject";

    @JsonProperty("@id")
    private String id;

    private String name;

    private String encodingFormat;

    private String contentSize;

    private String sha256;

    private String url;

    private ContextualEntity license;
}
