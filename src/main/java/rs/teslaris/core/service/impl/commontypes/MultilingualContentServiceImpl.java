package rs.teslaris.core.service.impl.commontypes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Service
@RequiredArgsConstructor
public class MultilingualContentServiceImpl implements MultilingualContentService {

    private final LanguageTagService languageTagService;


    @Transactional
    @Override
    public Set<MultiLingualContent> getMultilingualContent(
        List<MultilingualContentDTO> multilingualContentDTO) {
        return multilingualContentDTO.stream().map(multilingualContent -> {
            var languageTag = languageTagService.findOne(
                multilingualContent.getLanguageTagId());
            return new MultiLingualContent(
                languageTag,
                multilingualContent.getContent(),
                multilingualContent.getPriority()
            );
        }).collect(Collectors.toSet());
    }

    @Override
    public Set<MultiLingualContent> deepCopy(Set<MultiLingualContent> content) {
        var returnContent = new HashSet<MultiLingualContent>();
        content.forEach(mc -> {
            var copiedContent =
                new MultiLingualContent(mc.getLanguage(), mc.getContent(), mc.getPriority());
            returnContent.add(copiedContent);
        });

        return returnContent;
    }

    @Override
    public void buildLanguageStrings(StringBuilder serbianBuilder,
                                     StringBuilder otherLanguagesBuilder,
                                     Set<MultiLingualContent> contentList,
                                     boolean popEnglishFirst) {
        contentList.forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                serbianBuilder.append(content.getContent()).append(" | ");
            } else {
                if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.ENGLISH) &&
                    popEnglishFirst) {
                    otherLanguagesBuilder.insert(0, " | ").insert(0, content.getContent());
                } else {
                    otherLanguagesBuilder.append(content.getContent()).append(" | ");
                }
            }
        });
    }
}
