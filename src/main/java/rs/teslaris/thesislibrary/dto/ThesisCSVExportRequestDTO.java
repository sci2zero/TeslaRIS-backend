package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequestDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisCSVExportRequestDTO extends DocumentCSVExportRequestDTO {

    @Valid
    private ThesisSearchRequestDTO thesisSearchRequest;
}
