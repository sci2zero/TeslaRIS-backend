package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSVExportRequest {

    private List<String> columns;

    private List<Integer> exportEntityIds;

    private Boolean exportMaxPossibleAmount;

    private Integer bulkExportOffset;

    private String exportLanguage;
}
