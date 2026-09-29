package rs.teslaris.migrator.converter.hydrator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

/**
 * Shared conversion helpers for the hydrator source: language tags, dates, and the synthetic keys
 * used where the source provides no identifier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HydratorConversionUtil {

    /**
     * ISO 639-2 codes used by curricula, mapped onto the ISO 639-1 codes of the language
     * registry. The source sometimes lists the bibliographic and terminology forms together
     * ({@code FRA/FRE}); each form is tried.
     */
    private static final Map<String, String> LANGUAGE_CODES = Map.ofEntries(
        Map.entry("ENG", "EN"),
        Map.entry("POR", "PT"),
        Map.entry("SPA", "ES"),
        Map.entry("FRA", "FR"),
        Map.entry("FRE", "FR"),
        Map.entry("ITA", "IT"),
        Map.entry("DEU", "DE"),
        Map.entry("GER", "DE"),
        Map.entry("RUS", "RU"),
        Map.entry("HRV", "HR"),
        Map.entry("HUN", "HU"),
        Map.entry("SLV", "SL"),
        Map.entry("SRP", "SR")
    );

    private final LanguageTagService languageTagService;

    private final LanguageService languageService;

    // The language registry is small and static, while every curriculum looks it up
    private final Map<String, Optional<Integer>> languageIds = new ConcurrentHashMap<>();


    public List<MultilingualContentDTO> multilingualContent(String content, String language) {
        if (Objects.isNull(content) || content.isBlank()) {
            return List.of();
        }

        var languageTagValue = resolveLanguageTagValue(language);
        var dto = new MultilingualContentDTO();
        dto.setContent(content.trim());
        dto.setLanguageTag(languageTagValue);
        dto.setPriority(1);

        try {
            var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);

            if (Objects.nonNull(languageTag) && Objects.nonNull(languageTag.getId())) {
                dto.setLanguageTagId(languageTag.getId());
            }
        } catch (Exception e) {
            log.warn("Unknown language tag '{}', falling back to no explicit tag id.",
                languageTagValue);
        }

        return List.of(dto);
    }

    /**
     * Id of the registry language for a curriculum language code, or {@code null} when the code
     * has no mapping or the registry lacks that language.
     */
    public Integer languageId(String sourceCode) {
        if (Objects.isNull(sourceCode) || sourceCode.isBlank()) {
            return null;
        }

        return Arrays.stream(sourceCode.split("/"))
            .map(code -> LANGUAGE_CODES.get(code.trim().toUpperCase(Locale.ROOT)))
            .filter(Objects::nonNull)
            .map(code -> languageIds.computeIfAbsent(code, this::findLanguageId))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(null);
    }

    private Optional<Integer> findLanguageId(String code) {
        try {
            return Optional.ofNullable(languageService.findLanguageByCode(code).getId());
        } catch (NotFoundException e) {
            log.warn("Language '{}' is not in the language registry.", code);
            return Optional.empty();
        }
    }

    public FlexibleDateDTO flexibleDate(HydratorCVModel.DateInfo dateInfo) {
        if (Objects.isNull(dateInfo)) {
            return null;
        }

        var year = parseInteger(dateInfo.year());

        if (Objects.isNull(year)) {
            return null;
        }

        return new FlexibleDateDTO(year, parseInteger(dateInfo.month()),
            parseInteger(dateInfo.day()), null);
    }

    public Integer parseInteger(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Institutions in the curriculum payload carry no identifier, so their identity has to be
     * synthesised from the name. See the open question in the design document - this is the weakest
     * link in the pipeline and should be validated against real data.
     */
    public String institutionKey(HydratorCVModel.Institution institution) {
        if (Objects.isNull(institution) || Objects.isNull(institution.name())) {
            return null;
        }

        return normalise(institution.name());
    }

    public String normalise(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String resolveLanguageTagValue(String language) {
        if (Objects.isNull(language) || language.isBlank()) {
            return LanguageAbbreviations.ENGLISH;
        }

        // Curricula carry locales such as "pt_PT" / "en_GB"; tags are the bare language code
        return language.trim().split("[_-]")[0].toUpperCase(Locale.ROOT);
    }
}
