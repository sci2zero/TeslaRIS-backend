package rs.teslaris.core.harvester.converter.commontypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.harvester.common.MultilingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;

@Component
@RequiredArgsConstructor
public class MultilingualContentConverter {

    private final LanguageTagService languageTagService;


    public List<MultilingualContentDTO> toDTO(List<MultilingualContent> multilingualContent) {
        var result = new ArrayList<MultilingualContentDTO>();

        multilingualContent.forEach((mc) -> {
            var dto = new MultilingualContentDTO();
            var languageTagValue = mc.getLang().trim().toUpperCase();
            var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);
            if (!Objects.nonNull(languageTag.getId())) {
                return;
            }
            dto.setLanguageTagId(languageTag.getId());
            dto.setLanguageTag(languageTagValue);
            dto.setContent(mc.getValue());
            dto.setPriority(1);

            result.add(dto);
        });

        return result;
    }
}
