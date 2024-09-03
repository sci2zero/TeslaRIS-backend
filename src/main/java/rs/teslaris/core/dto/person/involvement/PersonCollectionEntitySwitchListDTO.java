package rs.teslaris.core.dto.person.involvement;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonCollectionEntitySwitchListDTO {

    private List<Integer> entityIds;
}
