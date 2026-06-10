package rs.teslaris.importer.utility;

import java.util.List;
import java.util.Map;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.util.LocalizationUtil;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;

@Component
public class CommonHarvestUtility {

    private static LanguageDetector languageDetector;


    public CommonHarvestUtility(LanguageDetector languageDetector) {
        CommonHarvestUtility.languageDetector = languageDetector;
    }

    public static void updateContributorEntryCount(DocumentImport doc,
                                                   List<String> contributorIdentifiers,
                                                   Map<Integer, Integer> newEntriesCount,
                                                   PersonService personService) {
        for (var identifier : contributorIdentifiers) {
            var userOpt = personService.findUserByIdentifier(identifier);
            userOpt.ifPresent(user -> {
                var contributorId = user.getId();
                doc.getImportUsersId().add(contributorId);
                newEntriesCount.merge(contributorId, 1, Integer::sum);
            });
        }
    }

    private static String detectLanguage(String text) {
        return languageDetector.detect(text).getLanguage().toUpperCase();
    }

    public static MultilingualContent createMultilingualContent(String text) {
        var language = detectLanguage(text);
        if (LanguageAbbreviations.CROATIAN.equals(language)) {
            language = LanguageAbbreviations.SERBIAN;
        }

        if (!List.of(LanguageAbbreviations.SERBIAN,
            LanguageAbbreviations.ENGLISH, LanguageAbbreviations.GERMAN,
            LanguageAbbreviations.SPANISH, LanguageAbbreviations.PORTUGUESE,
            LanguageAbbreviations.FRENCH, LanguageAbbreviations.HUNGARIAN,
            LanguageAbbreviations.ITALIAN, LanguageAbbreviations.RUSSIAN).contains(language)) {
            language = LanguageAbbreviations.ENGLISH;
        }

        return new MultilingualContent(
            language, text, 1
        );
    }

    public static MultilingualContent createProceedingsName(String conferenceName) {
        var proceedingsName = createMultilingualContent(conferenceName);
        var locale = proceedingsName.getLanguageTag();
        proceedingsName.setContent(
            LocalizationUtil.getMessage("proceedings.prefix", new Object[] {}, locale) + " " +
                proceedingsName.getContent());

        return proceedingsName;
    }
}
