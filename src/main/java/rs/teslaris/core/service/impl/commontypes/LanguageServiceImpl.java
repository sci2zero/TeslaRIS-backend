package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.LanguageMigrationDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.language.SerbianTransliteration;

@Service
@RequiredArgsConstructor
@Traceable
public class LanguageServiceImpl extends JPAServiceImpl<Language> implements LanguageService {

    private final LanguageRepository languageRepository;

    private final LanguageTagRepository languageTagRepository;

    private final MultilingualContentService multilingualContentService;


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
    public Language findLanguageByCode(String languageCode) {
        return languageRepository.getLanguageByLanguageCode(languageCode)
            .orElseThrow(() -> new NotFoundException("Language with given code does not exist."));
    }

    @Override
    @Transactional
    public void migrateLanguage(LanguageMigrationDTO languageMigrationDTO) {
        var existingLanguage = languageRepository.getLanguageByLanguageCode(
            languageMigrationDTO.languageCode().toUpperCase());

        Language language;
        if (existingLanguage.isPresent()) {
            language = existingLanguage.get();
            language.getName().clear();
        } else {
            language = new Language();
            language.setLanguageCode(languageMigrationDTO.languageCode().toUpperCase());
        }

        language.getName().addAll(
            multilingualContentService.getMultilingualContent(languageMigrationDTO.name()));
        addCyrillicContentIfPossible(languageMigrationDTO, language);
        save(language);
    }

    private void addCyrillicContentIfPossible(LanguageMigrationDTO languageMigrationDTO,
                                              Language language) {
        languageTagRepository.findLanguageTagByLanguageTag(LanguageAbbreviations.SERBIAN_CYRILLIC)
            .ifPresent(srCyr -> {
                languageMigrationDTO.name().stream()
                    .filter(
                        mc -> mc.getLanguageTag().equalsIgnoreCase(LanguageAbbreviations.SERBIAN))
                    .findFirst().ifPresent(serbianContent ->
                        language.getName().add(new MultiLingualContent(
                            srCyr,
                            SerbianTransliteration.toCyrillic(serbianContent.getContent()),
                            3
                        )));
            });
    }
}
