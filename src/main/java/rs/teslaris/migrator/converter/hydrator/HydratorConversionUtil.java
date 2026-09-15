package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
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

    private final LanguageTagService languageTagService;


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

        return language.trim().toUpperCase(Locale.ROOT);
    }
}
