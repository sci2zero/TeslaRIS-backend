package rs.teslaris.core.dto.commontypes;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

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

    private List<DocumentPublicationType> allowedTypes = new ArrayList<>();

    private Integer institutionId;

    private Integer commissionId;
}
