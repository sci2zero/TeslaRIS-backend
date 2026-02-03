package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.ExhibitionDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedExhibitionDTO {

    private ExhibitionDTO leftExhibition;

    private ExhibitionDTO rightExhibition;
}
