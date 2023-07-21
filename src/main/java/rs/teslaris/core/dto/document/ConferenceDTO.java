package rs.teslaris.core.dto.document;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceDTO extends EventDTO {

    private Integer id;

    private String number;

    private String fee;
}
