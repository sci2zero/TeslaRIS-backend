package rs.teslaris.core.service.impl.commontypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
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
                multilingualContent.getContent().trim(),
                multilingualContent.getPriority()
            );
        }).collect(Collectors.toSet());
    }

    public Set<MultiLingualContent> getMultilingualContentAndSetDefaultsIfNonExistent(
        List<MultilingualContentDTO> multilingualContentDTOs) {

        var multilingualContent = multilingualContentDTOs.stream()
            .map(mc -> new MultiLingualContent(
                languageTagService.findOne(mc.getLanguageTagId()),
                mc.getContent().trim(),
                mc.getPriority()))
            .collect(Collectors.toCollection(ArrayList::new));

        if (multilingualContent.isEmpty()) {
            return new HashSet<>();
        }

        ensureLanguageContentExists(multilingualContent, LanguageAbbreviations.ENGLISH,
            LanguageAbbreviations.SERBIAN);
        ensureLanguageContentExists(multilingualContent, LanguageAbbreviations.SERBIAN,
            LanguageAbbreviations.ENGLISH);

        return new HashSet<>(multilingualContent);
    }

    private void ensureLanguageContentExists(List<MultiLingualContent> multilingualContent,
                                             String targetLanguageTag,
                                             String fallbackLanguageTag) {

        if (multilingualContent.stream()
            .noneMatch(
                mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(targetLanguageTag))) {

            var targetLanguage = languageTagService.findLanguageTagByValue(targetLanguageTag);
            var fallbackContent = multilingualContent.stream()
                .filter(
                    mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(fallbackLanguageTag))
                .findFirst()
                .orElse(multilingualContent.getFirst());

            multilingualContent.add(new MultiLingualContent(
                targetLanguage,
                fallbackContent.getContent(),
                fallbackContent.getPriority()));
        }
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
        buildLanguageStringsInternal(serbianBuilder, otherLanguagesBuilder, contentList,
            popEnglishFirst, content -> content);
    }

    @Override
    public void buildLanguageStringsFromHTMLMC(StringBuilder serbianBuilder,
                                               StringBuilder otherLanguagesBuilder,
                                               Set<MultiLingualContent> contentList,
                                               boolean popEnglishFirst) {
        buildLanguageStringsInternal(serbianBuilder, otherLanguagesBuilder, contentList,
            popEnglishFirst, this::html2text);
    }

    private void buildLanguageStringsInternal(StringBuilder serbianBuilder,
                                              StringBuilder otherLanguagesBuilder,
                                              Set<MultiLingualContent> contentList,
                                              boolean popEnglishFirst,
                                              Function<String, String> contentTransformer) {
        contentList.forEach(content -> {
            String transformedContent = contentTransformer.apply(content.getContent());
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                serbianBuilder.append(transformedContent).append(" | ");
            } else {
                if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.ENGLISH) &&
                    popEnglishFirst) {
                    otherLanguagesBuilder.insert(0, " | ").insert(0, transformedContent);
                } else {
                    otherLanguagesBuilder.append(transformedContent).append(" | ");
                }
            }
        });
    }

    private String html2text(String html) {
        return Jsoup.parse(html).text();
    }
}
