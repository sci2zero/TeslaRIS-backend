package rs.teslaris.dbinitialization;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.seeding.CsvDataLoader;

/**
 * Seeds countries from {@code countries.csv}, plus the historical Yugoslavia entry.
 * <p>
 * Countries are saved in CSV order followed by Yugoslavia, which determines the generated IDs.
 */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CountryDataInitializer {

    private final CountryRepository countryRepository;

    private final LanguageTagRepository languageTagRepository;

    private final CsvDataLoader csvDataLoader;


    public void initializeCountries(LanguageTag serbianTag, LanguageTag englishTag) {
        csvDataLoader.loadData("countries.csv", this::processCountryLine, ',');

        var yugoslavia = new Country();
        yugoslavia.setCode("YU");
        yugoslavia.setName(Set.of(new MultiLingualContent(serbianTag, "Jugoslavija", 1),
            new MultiLingualContent(englishTag, "Yugoslavia", 2)));
        countryRepository.save(yugoslavia);
    }

    private void processCountryLine(String[] line) {
        var country = new Country();
        country.setCode(line[0]);

        var names = new HashSet<MultiLingualContent>();
        var nameList = line[1].split(";");

        int cyrillicCount = 0;
        for (int i = 0; i < nameList.length; i++) {
            var nameParts = nameList[i].split("@");
            if (nameParts.length == 2) {
                var content = nameParts[0].trim();
                var languageTagCode = nameParts[1].trim().toUpperCase();
                var basePriority = i + 1 + cyrillicCount;

                addMultilingualContent(languageTagCode, content, names, basePriority);

                if (languageTagCode.equals(LanguageAbbreviations.SERBIAN)) {
                    var cyrillicContent = SerbianTransliteration.toCyrillic(content);
                    addMultilingualContent(LanguageAbbreviations.SERBIAN_CYRILLIC, cyrillicContent,
                        names, basePriority + 1);
                    cyrillicCount++;
                }
            }
        }
        country.setName(names);
        country.setProcessedName("");
        country.getName().forEach(name -> {
            country.setProcessedName(country.getProcessedName() + " " +
                StringUtil.performSimpleLatinPreprocessing(name.getContent()));
        });
        countryRepository.save(country);
    }

    private void addMultilingualContent(String languageTagCode, String content,
                                        HashSet<MultiLingualContent> mc, int priority) {
        var languageTag = languageTagRepository
            .findLanguageTagByLanguageTag(languageTagCode)
            .orElseGet(() -> {
                var newLanguageTag = new LanguageTag();
                newLanguageTag.setLanguageTag(languageTagCode);
                newLanguageTag.setDisplay(languageTagCode);
                log.info("Created new language tag: {}", languageTagCode);
                return languageTagRepository.save(newLanguageTag);
            });

        var multilingualContent =
            new MultiLingualContent(languageTag, content, priority);
        mc.add(multilingualContent);
    }
}
