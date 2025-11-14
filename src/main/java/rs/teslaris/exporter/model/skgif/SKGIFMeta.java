package rs.teslaris.exporter.model.skgif;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SKGIFMeta {

    @JsonProperty("count")
    private long count;

    @JsonProperty("page")
    private int page;

    @JsonProperty("page_size")
    private int size;
}
