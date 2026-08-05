package rs.teslaris.migrator.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Deserialisable stand-in for a Spring {@code Page} response body.
 * <p>
 * {@code PageImpl} has no default constructor, so Jackson (and therefore Feign) cannot read it
 * directly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestPage<T>(
    List<T> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {

    @JsonCreator
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int number,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long totalElements,
                    @JsonProperty("totalPages") int totalPages,
                    @JsonProperty("last") boolean last) {
        this.content = Objects.isNull(content) ? List.of() : content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }
}
