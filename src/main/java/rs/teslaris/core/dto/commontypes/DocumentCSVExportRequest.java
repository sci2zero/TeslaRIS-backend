package rs.teslaris.core.dto.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCSVExportRequest extends CSVExportRequest {

    private Boolean apa;

    private Boolean mla;

    private Boolean chicago;

    private Boolean harvard;

    private Boolean vancouver;
}
