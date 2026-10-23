package rs.teslaris.dbinitialization;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;

/**
 * Seeds language tags and languages.
 * <p>
 * The order of the {@code saveAll} lists below determines the generated IDs, which integration
 * tests rely on (english tag is 1, serbian tag is 2). Both lists are kept exactly as they were, so
 * entries may only ever be appended - never reordered or removed.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class LanguageDataInitializer {

    private final LanguageTagRepository languageTagRepository;

    private final LanguageRepository languageRepository;


    public SeededLanguages initializeLanguages() {
        // LANGUAGE TAGS
        var englishTag = new LanguageTag(LanguageAbbreviations.ENGLISH, "English");
        var serbianTag = new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski");
        var serbianCyrillicTag = new LanguageTag(LanguageAbbreviations.SERBIAN_CYRILLIC, "Српски");
        var hungarianTag = new LanguageTag(LanguageAbbreviations.HUNGARIAN, "Magyar");
        var germanTag = new LanguageTag(LanguageAbbreviations.GERMAN, "Deutsch");
        var frenchTag = new LanguageTag(LanguageAbbreviations.FRENCH, "Français");
        var spanishTag = new LanguageTag(LanguageAbbreviations.SPANISH, "Español");
        var russianTag = new LanguageTag(LanguageAbbreviations.RUSSIAN, "Русский");
        var croatianTag = new LanguageTag(LanguageAbbreviations.CROATIAN, "Croatian");
        var italianTag = new LanguageTag(LanguageAbbreviations.ITALIAN, "Italian");
        var slovenianTag = new LanguageTag(LanguageAbbreviations.SLOVENIAN, "Slovenian");
        var portugueseTag = new LanguageTag(LanguageAbbreviations.PORTUGUESE, "Português");
        languageTagRepository.saveAll(
            List.of(englishTag, serbianTag, hungarianTag, germanTag, frenchTag, spanishTag,
                russianTag, croatianTag, italianTag, slovenianTag, serbianCyrillicTag,
                portugueseTag));

        // LANGUAGES
        var serbianLanguage = new Language();
        serbianLanguage.setLanguageCode(LanguageAbbreviations.SERBIAN);
        serbianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Srpski", 1))));
        var englishLanguage = new Language();
        englishLanguage.setLanguageCode(LanguageAbbreviations.ENGLISH);
        englishLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Engleski", 1))));
        // We will maybe need YU, I don't know
        var yuLanguage = new Language();
        yuLanguage.setLanguageCode(LanguageAbbreviations.YUGOSLAV);
        yuLanguage.setDeleted(true);
        var germanLanguage = new Language();
        germanLanguage.setLanguageCode(LanguageAbbreviations.GERMAN);
        germanLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Nemački", 1))));
        var frenchLanguage = new Language();
        frenchLanguage.setLanguageCode(LanguageAbbreviations.FRENCH);
        frenchLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Francuski", 1))));
        var spanishLanguage = new Language();
        spanishLanguage.setLanguageCode(LanguageAbbreviations.SPANISH);
        spanishLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Španski", 1))));
        var russianLanguage = new Language();
        russianLanguage.setLanguageCode(LanguageAbbreviations.RUSSIAN);
        russianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Ruski", 1))));
        var croatianLanguage = new Language();
        croatianLanguage.setLanguageCode(LanguageAbbreviations.CROATIAN);
        croatianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Hrvatski", 1))));
        var italianLanguage = new Language();
        italianLanguage.setLanguageCode(LanguageAbbreviations.ITALIAN);
        italianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Italijanski", 1))));
        var slovenianLanguage = new Language();
        slovenianLanguage.setLanguageCode(LanguageAbbreviations.SLOVENIAN);
        slovenianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Slovenački", 1))));
        var hungarianLanguage = new Language();
        hungarianLanguage.setLanguageCode(LanguageAbbreviations.HUNGARIAN);
        hungarianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Mađarski", 1))));
        var portugueseLanguage = new Language();
        portugueseLanguage.setLanguageCode(LanguageAbbreviations.PORTUGUESE);
        portugueseLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Portugalski", 1))));

        languageRepository.saveAll(
            List.of(serbianLanguage, englishLanguage, yuLanguage, germanLanguage, frenchLanguage,
                spanishLanguage, russianLanguage, croatianLanguage, italianLanguage,
                slovenianLanguage, hungarianLanguage, portugueseLanguage));

        return new SeededLanguages(englishTag, serbianTag, serbianCyrillicTag, germanTag,
            serbianLanguage);
    }

    public record SeededLanguages(
        LanguageTag englishTag,
        LanguageTag serbianTag,
        LanguageTag serbianCyrillicTag,
        LanguageTag germanTag,
        Language serbianLanguage
    ) {
    }
}
