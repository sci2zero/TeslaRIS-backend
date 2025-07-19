package rs.teslaris.core.service.impl.commontypes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.LanguageTagDTO;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.LanguageTagReferenceConstraintException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Traceable
public class LanguageTagServiceImpl extends JPAServiceImpl<LanguageTag>
    implements LanguageTagService {

    private final LanguageTagRepository languageTagRepository;

    @Override
    protected JpaRepository<LanguageTag, Integer> getEntityRepository() {
        return languageTagRepository;
    }

    @Override
    public Page<LanguageTagResponseDTO> searchLanguageTags(Pageable pageable,
                                                           String searchExpression) {
        if (searchExpression.equals("*")) {
            searchExpression = "";
        }

        return languageTagRepository.searchLanguageTags(searchExpression, pageable).map(
            lt -> new LanguageTagResponseDTO(lt.getId(), lt.getLanguageTag(), lt.getDisplay()));
    }

    @Override
    @Deprecated(forRemoval = true)
    public LanguageTag findLanguageTagById(Integer languageTagId) {
        return languageTagRepository.findById(languageTagId)
            .orElseThrow(() -> new NotFoundException("Language tag with given ID does not exist."));
    }

    @Override
    public LanguageTag findLanguageTagByValue(String languageTag) {
        return languageTagRepository.findLanguageTagByLanguageTag(languageTag)
            .orElse(new LanguageTag());
    }

    @Override
    public LanguageTag createLanguageTag(LanguageTagDTO languageTagDTO) {
        var newLanguageTag = new LanguageTag();
        setCommonFields(newLanguageTag, languageTagDTO);
        return save(newLanguageTag);
    }

    @Override
    public void updateLanguageTag(Integer languageTagId, LanguageTagDTO languageTagDTO) {
        var languageTagToUpdate = findOne(languageTagId);
        setCommonFields(languageTagToUpdate, languageTagDTO);
        save(languageTagToUpdate);
    }

    @Override
    public void deleteLanguageTag(Integer languageTagId) {
        if (languageTagRepository.isUsedInContent(languageTagId)) {
            throw new LanguageTagReferenceConstraintException("languageTagInUse");
        }
        delete(languageTagId);
    }

    private void setCommonFields(LanguageTag languageTag, LanguageTagDTO languageTagDTO) {
        languageTag.setDisplay(languageTagDTO.getDisplay());
        languageTag.setLanguageTag(languageTagDTO.getLanguageCode());
    }

    @Override
    public List<LanguageTagResponseDTO> getUISupportedLanguages() {
        var availableLocales = new HashSet<String>();
        try {
            var resources = getResourcesForMessages();

            for (var resource : resources) {
                var filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }

                var langCode = filename.replace("messages_", "").replace(".properties", "");
                availableLocales.add(langCode.toUpperCase());
                availableLocales.add(langCode.toUpperCase() + "-CYR"); // Support cyrillic variants
            }
        } catch (IOException e) {
            throw new LoadingException("Unable to scan for message files.");
        }

        return languageTagRepository.findAll().stream()
            .filter(language -> availableLocales.contains(language.getLanguageTag().toUpperCase()))
            .map(language -> new LanguageTagResponseDTO(
                language.getId(),
                language.getLanguageTag(),
                language.getDisplay()))
            .collect(Collectors.toList());
    }

    protected Resource[] getResourcesForMessages() throws IOException {
        return new PathMatchingResourcePatternResolver()
            .getResources("classpath*:internationalization/messages_*.properties");
    }
}
