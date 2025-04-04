package rs.teslaris.thesislibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisCSVExportRequestDTO extends DocumentCSVExportRequest {

    private ThesisSearchRequestDTO thesisSearchRequest;
}
