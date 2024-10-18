package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.indexmodel.EntityType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReindexRequestDTO {
    List<EntityType> indexesToRepopulate;
}
