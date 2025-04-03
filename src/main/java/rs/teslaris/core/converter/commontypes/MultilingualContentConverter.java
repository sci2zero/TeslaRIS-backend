package rs.teslaris.core.converter.commontypes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

public class MultilingualContentConverter {

    public static List<MultilingualContentDTO> getMultilingualContentDTO(
        Set<MultiLingualContent> multilingualContent) {
        return multilingualContent.stream().map(mc ->
            new MultilingualContentDTO(
                mc.getLanguage().getId(),
                mc.getLanguage().getLanguageTag(),
                mc.getContent(),
                mc.getPriority()
            )).collect(Collectors.toList());
    }

    public static String getLocalizedContent(
        Set<MultiLingualContent> multilingualContent, String locale) {
        var content = multilingualContent.stream()
            .filter(mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(locale)).findFirst();

        if (content.isEmpty()) {
            content = multilingualContent.stream().findAny();
        }

        if (content.isPresent()) {
            return content.get().getContent();
        } else {
            return "";
        }
    }
}
