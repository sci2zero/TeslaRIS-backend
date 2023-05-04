package rs.teslaris.core.converter.commontypes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Component
public class MultilingualContentToMultilingualContentDTO {

    public List<MultilingualContentDTO> getMultilingualContentDTO(
        Set<MultiLingualContent> multilingualContent) {
        return multilingualContent.stream().map(mc ->
            new MultilingualContentDTO(
                mc.getLanguage().getId(),
                mc.getContent(),
                mc.getPriority()
            )).collect(Collectors.toList());
    }
}
