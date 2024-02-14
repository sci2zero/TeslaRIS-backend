package rs.teslaris.core.importer.converter.commontypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.importer.common.MultilingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Component
@RequiredArgsConstructor
public class MultilingualContentConverter {

    private final LanguageTagService languageTagService;

    private final LanguageDetector languageDetector;


    public List<MultilingualContentDTO> toDTO(List<MultilingualContent> multilingualContent) {
        var result = new ArrayList<MultilingualContentDTO>();

        multilingualContent.forEach((mc) -> {
            var dto = new MultilingualContentDTO();
            var languageTagValue = mc.getLang().trim().toUpperCase();

            if (languageTagValue.isEmpty()) {
                languageTagValue = LanguageAbbreviations.ENGLISH;
            }

            if (languageTagValue.equals(LanguageAbbreviations.CROATIAN)) {
                languageTagValue = LanguageAbbreviations.SERBIAN;
            }

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

    public List<MultilingualContentDTO> toDTO(String content) {
        var result = new ArrayList<MultilingualContentDTO>();
        if (!Objects.nonNull(content) || content.trim().isEmpty()) {
            return result;
        }

        var contentLanguageDetected = languageDetector.detect(content).getLanguage().toUpperCase();
        if (contentLanguageDetected.equals(LanguageAbbreviations.CROATIAN)) {
            contentLanguageDetected = LanguageAbbreviations.SERBIAN;
        }

        if (content.length() < 50 &&
            !contentLanguageDetected.equals(LanguageAbbreviations.SERBIAN) &&
            !contentLanguageDetected.equals(LanguageAbbreviations.ENGLISH)) {
            contentLanguageDetected = LanguageAbbreviations.ENGLISH;
        }

        var dto = new MultilingualContentDTO();
        var languageTag = languageTagService.findLanguageTagByValue(contentLanguageDetected);
        if (!Objects.nonNull(languageTag.getId())) {
            return result;
        }

        dto.setLanguageTagId(languageTag.getId());
        dto.setLanguageTag(contentLanguageDetected);
        dto.setContent(content);
        dto.setPriority(1);

        result.add(dto);
        return result;
    }
}
