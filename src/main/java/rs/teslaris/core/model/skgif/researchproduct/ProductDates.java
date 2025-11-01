package rs.teslaris.core.model.skgif.researchproduct;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
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
public class ProductDates {

    @JsonProperty("acceptance")
    private LocalDateTime acceptance;

    @JsonProperty("access")
    private LocalDateTime access;

    @JsonProperty("copyright")
    private LocalDateTime copyright;

    @JsonProperty("creation")
    private LocalDateTime creation;

    @JsonProperty("received")
    private LocalDateTime received;

    @JsonProperty("decision")
    private LocalDateTime decision;

    @JsonProperty("deposit")
    private LocalDateTime deposit;

    @JsonProperty("embargo")
    private LocalDateTime embargo;

    @JsonProperty("modified")
    private LocalDateTime modified;

    @JsonProperty("distribution")
    private LocalDateTime distribution;

    @JsonProperty("publication")
    private LocalDateTime publication;

    @JsonProperty("retraction")
    private LocalDateTime retraction;
}
