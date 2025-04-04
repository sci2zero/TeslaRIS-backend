package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;

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

    private ExportFileType exportFileType;

    private ExportableEndpointType endpointType;

    private List<String> endpointTokenParameters;
}
