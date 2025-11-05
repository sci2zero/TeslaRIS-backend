package rs.teslaris.exporter.model.skgif;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SKGIFMeta {

    @JsonProperty("count")
    private long count;

    @JsonProperty("page")
    private int page;

    @JsonProperty("page_size")
    private int size;
}
