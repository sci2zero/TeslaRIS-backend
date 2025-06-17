package rs.teslaris.core.service.impl.commontypes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Traceable
public class LanguageServiceImpl extends JPAServiceImpl<Language> implements LanguageService {

    private final LanguageRepository languageRepository;

    @Override
    protected JpaRepository getEntityRepository() {
        return languageRepository;
    }

    @Override
    public Language findLanguageById(Integer id) {
        return languageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Language with given ID does not exist."));
    }

    @Override
    public List<LanguageResponseDTO> getUISupportedLanguages() {
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
            }
        } catch (IOException e) {
            throw new LoadingException("Unable to scan for message files.");
        }

        return languageRepository.findAll().stream()
            .filter(language -> availableLocales.contains(language.getLanguageCode().toUpperCase()))
            .map(language -> new LanguageResponseDTO(
                language.getId(),
                language.getLanguageCode(),
                MultilingualContentConverter.getMultilingualContentDTO(language.getName())))
            .collect(Collectors.toList());
    }

    protected Resource[] getResourcesForMessages() throws IOException {
        return new PathMatchingResourcePatternResolver()
            .getResources("classpath*:internationalization/messages_*.properties");
    }
}
