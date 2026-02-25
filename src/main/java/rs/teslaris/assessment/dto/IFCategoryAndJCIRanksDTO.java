package rs.teslaris.assessment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.util.functional.Pair;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IFCategoryAndJCIRanksDTO {

    private String category;

    private List<Pair<Integer, String>> if2Ranks;

    private List<Pair<Integer, String>> if5Ranks;

    private List<Pair<Integer, String>> jciRanks;
}
