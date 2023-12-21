package rs.teslaris.core.service.impl.commontypes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    public void buildLanguageStrings(StringBuilder serbianBuilder,
                                     StringBuilder otherLanguagesBuilder,
                                     Set<MultiLingualContent> contentList) {
        contentList.forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                serbianBuilder.append(content.getContent()).append(" | ");
            } else {
                otherLanguagesBuilder.append(content.getContent()).append(" | ");
            }
        });
    }
}
