package rs.teslaris.core.dto.deduplication;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedDocumentsDTO {

    private List<Integer> leftProofs;

    private List<Integer> rightProofs;

    private List<Integer> leftFileItems;

    private List<Integer> rightFileItems;
}
