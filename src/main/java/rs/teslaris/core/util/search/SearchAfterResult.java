package rs.teslaris.core.util.search;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchAfterResult {
    private List<Integer> documentIds;
    private Object[] searchAfterValues;
}
