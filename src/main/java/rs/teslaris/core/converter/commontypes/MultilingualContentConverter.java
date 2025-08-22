package rs.teslaris.core.converter.commontypes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.Pair;

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

    public static String getLocalizedContent(Set<MultiLingualContent> multilingualContent,
                                             String locale) {
        var content = getContentWithAnyFallback(multilingualContent, locale);

        if (content.isPresent()) {
            return content.get().getContent();
        } else {
            return "";
        }
    }

    public static Pair<String, String> getLocalizedContentWithLocale(
        Set<MultiLingualContent> multilingualContent, String locale) {
        var content = getContentWithAnyFallback(multilingualContent, locale);

        return content.map(multiLingualContent -> new Pair<>(multiLingualContent.getContent(),
                multiLingualContent.getLanguage().getLanguageTag()))
            .orElseGet(() -> new Pair<>("", ""));
    }

    private static Optional<MultiLingualContent> getContentWithAnyFallback(
        Set<MultiLingualContent> multilingualContent, String locale) {
        return multilingualContent.stream()
            .filter(mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(locale))
            .findFirst()
            .or(() -> {
                if (locale.contains("-")) {
                    var base = locale.split("-")[0];
                    return multilingualContent.stream()
                        .filter(mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(base))
                        .findFirst();
                }
                return Optional.empty();
            })
            .or(() -> multilingualContent.stream().findAny());
    }
}
