package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.SoftwareDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedSoftwareDTO {

    private SoftwareDTO leftSoftware;

    private SoftwareDTO rightSoftware;
}
