package rs.teslaris.core.assessment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.util.Pair;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IFTableContentDTO {

    private String category;

    private List<Pair<Integer, String>> if2Values;

    private List<Pair<Integer, String>> if2Ranks;

    private List<Pair<Integer, String>> if5Values;

    private List<Pair<Integer, String>> if5Ranks;
}
