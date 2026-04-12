package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.OtherEventDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedOtherEventDTO {

    private OtherEventDTO leftOtherEvent;

    private OtherEventDTO rightOtherEvent;
}
