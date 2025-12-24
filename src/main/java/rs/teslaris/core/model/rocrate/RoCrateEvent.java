package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoCrateEvent {

    @JsonProperty("@type")
    private final String type;

    @JsonProperty("@id")
    private String id;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private String location;

    private String keywords;

    private List<ContextualEntity> subEvents = new ArrayList<>();

    private List<ContextualEntity> superEvents = new ArrayList<>();

    private String identifier;

    private String url;


    public RoCrateEvent() {
        this.type = "Event";
    }
}
