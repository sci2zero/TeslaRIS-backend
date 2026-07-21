package rs.teslaris.core.converter.commontypes;

import java.util.Objects;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.model.commontypes.FlexibleDate;

public class FlexibleDateConverter {

    public static FlexibleDateDTO toDTO(FlexibleDate flexibleDate) {
        if (Objects.isNull(flexibleDate)) {
            return null;
        }

        return new FlexibleDateDTO(
            flexibleDate.getYear(),
            flexibleDate.getMonth(),
            flexibleDate.getDay(),
            flexibleDate.getText()
        );
    }

    public static FlexibleDate fromDTO(FlexibleDateDTO dto) {
        if (Objects.isNull(dto)) {
            return new FlexibleDate();
        }

        return new FlexibleDate(
            dto.year(),
            dto.month(),
            dto.day(),
            dto.text()
        );
    }
}
