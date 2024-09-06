package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.DatasetDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedDatasetsDTO {

    private DatasetDTO leftDataset;

    private DatasetDTO rightDataset;
}
