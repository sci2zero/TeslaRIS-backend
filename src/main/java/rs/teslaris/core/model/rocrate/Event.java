package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @JsonProperty("@type")
    private final String type = "Event";

    @JsonProperty("@id")
    private String id;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private String location;

    private String keywords;

    private List<String> subEvents;

    private List<String> superEvents;

    private String identifier;

    private String url;
}
