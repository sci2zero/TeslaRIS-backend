package rs.teslaris.core.converter.commontypes;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;

@Component
public class StringToRelativeDateDTOConverter implements Converter<String, RelativeDateDTO> {

    @Override
    public RelativeDateDTO convert(@NotNull String source) {
        return RelativeDateDTO.parse(source);
    }
}
