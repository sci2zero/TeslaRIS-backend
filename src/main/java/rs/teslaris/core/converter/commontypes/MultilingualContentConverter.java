package rs.teslaris.core.converter.commontypes;

import java.util.List;
import java.util.Objects;
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
                                             String locale, String avoidLocale) {
        var content = getContentWithAnyFallback(multilingualContent, locale, avoidLocale);

        if (content.isPresent()) {
            return content.get().getContent();
        } else {
            return "";
        }
    }

    public static Pair<String, String> getLocalizedContentWithLocale(
        Set<MultiLingualContent> multilingualContent, String locale) {
        var content = getContentWithAnyFallback(multilingualContent, locale, null);

        return content.map(multiLingualContent -> new Pair<>(multiLingualContent.getContent(),
                multiLingualContent.getLanguage().getLanguageTag()))
            .orElseGet(() -> new Pair<>("", ""));
    }

    private static Optional<MultiLingualContent> getContentWithAnyFallback(
        Set<MultiLingualContent> multilingualContent, String locale, String avoidLocale) {
        return multilingualContent.stream()
            .filter(mc -> Objects.nonNull(mc.getLanguage()) &&
                mc.getLanguage().getLanguageTag().equalsIgnoreCase(locale))
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
            .or(() -> multilingualContent.stream().filter(mc -> Objects.isNull(avoidLocale) ||
                Objects.isNull(mc.getLanguage()) ||
                !mc.getLanguage().getLanguageTag().startsWith(avoidLocale)).findAny())
            .or(() -> multilingualContent.stream().findAny());
    }
}
