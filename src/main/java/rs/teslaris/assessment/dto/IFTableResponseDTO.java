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
public class IFTableResponseDTO {

    private List<Pair<Integer, String>> if2Values;

    private List<Pair<Integer, String>> if5Values;

    private List<IFCategoryRanksDTO> ifTableContent;
}
