package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.BookSeriesDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedBookSeriesDTO {

    private BookSeriesDTO leftBookSeries;

    private BookSeriesDTO rightBookSeries;
}
