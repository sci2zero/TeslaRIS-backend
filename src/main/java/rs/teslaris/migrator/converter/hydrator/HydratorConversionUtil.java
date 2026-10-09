package rs.teslaris.migrator.converter.hydrator;

import java.text.Normalizer;
import java.util.ArrayList;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.InvalidSourceValueException;

/**
 * Shared conversion helpers for the hydrator source: language tags, dates, and the synthetic keys
 * used where the source provides no identifier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HydratorConversionUtil {

    /**
     * ISO 639-2 codes used by curricula, mapped onto the ISO 639-1 codes of the language
     * registry. The source sometimes lists the bibliographic and terminology forms together
     * ({@code FRA/FRE}); each form is tried.
     */
    private static final Map<String, String> LANGUAGE_CODES = Map.ofEntries(
        Map.entry("ENG", "EN"),
        Map.entry("POR", "PT"),
        Map.entry("SPA", "ES"),
        Map.entry("FRA", "FR"),
        Map.entry("FRE", "FR"),
        Map.entry("ITA", "IT"),
        Map.entry("DEU", "DE"),
        Map.entry("GER", "DE"),
        Map.entry("RUS", "RU"),
        Map.entry("HRV", "HR"),
        Map.entry("HUN", "HU"),
        Map.entry("SLV", "SL"),
        Map.entry("SRP", "SR")
    );

    /**
     * Top-level FCT field names that differ from their EuroSciVoc counterparts.
     */
    private static final Map<String, String> RESEARCH_AREA_SYNONYMS = Map.of(
        "agrarian sciences", "agricultural sciences",
        "exact sciences", "natural sciences"
    );

    private static final String RESEARCH_AREA_LEVEL_SEPARATOR = " - ";

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    // Bibliographic initials trailing a surname ("Nikolaev AR", "Watanabe MA.", "METZ K J")
    private static final Pattern INITIALS = Pattern.compile("\\p{Lu}{1,3}");

    // Leading surname particles that author lists drop or keep inconsistently ("da Rocha")
    private static final Set<String> SURNAME_PARTICLES =
        Set.of("da", "de", "do", "dos", "das", "e", "del", "della", "di", "du", "van", "von",
            "der", "la", "le", "los");

    private final LanguageTagService languageTagService;

    private final LanguageService languageService;

    private final ResearchAreaService researchAreaService;

    // The language registry is small and static, while every curriculum looks it up
    private final Map<String, Optional<Integer>> languageIds = new ConcurrentHashMap<>();

    /**
     * Normalised English research area name to id, read in one query on first use - not at
     * startup, as the vocabulary is loaded asynchronously after the database is initialised.
     * Published immutable, so readers need no locking.
     */
    private volatile Map<String, Integer> researchAreaIdsByName;

    // Whole curriculum labels already resolved; only a few hundred distinct ones exist
    private final Map<String, Optional<ResearchAreaMatch>> researchAreaMatches =
        new ConcurrentHashMap<>();


    public List<MultilingualContentDTO> multilingualContent(String content, String language) {
        if (Objects.isNull(content) || content.isBlank()) {
            return List.of();
        }

        var languageTagValue = resolveLanguageTagValue(language);
        var dto = new MultilingualContentDTO();
        dto.setContent(content.trim());
        dto.setLanguageTag(languageTagValue);
        dto.setPriority(1);

        try {
            var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);

            if (Objects.nonNull(languageTag) && Objects.nonNull(languageTag.getId())) {
                dto.setLanguageTagId(languageTag.getId());
            }
        } catch (Exception e) {
            log.warn("Unknown language tag '{}', falling back to no explicit tag id.",
                languageTagValue);
        }

        return List.of(dto);
    }

    /**
     * Id of the registry language for a curriculum language code, or {@code null} when the code
     * has no mapping or the registry lacks that language.
     */
    public Integer languageId(String sourceCode) {
        if (Objects.isNull(sourceCode) || sourceCode.isBlank()) {
            return null;
        }

        return Arrays.stream(sourceCode.split("/"))
            .map(code -> LANGUAGE_CODES.get(code.trim().toUpperCase(Locale.ROOT)))
            .filter(Objects::nonNull)
            .map(code -> languageIds.computeIfAbsent(code, this::findLanguageId))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(null);
    }

    private Optional<Integer> findLanguageId(String code) {
        try {
            return Optional.ofNullable(languageService.findLanguageByCode(code).getId());
        } catch (NotFoundException e) {
            log.warn("Language '{}' is not in the language registry.", code);
            return Optional.empty();
        }
    }

    /**
     * Research area for a curriculum classification label such as
     * {@code "Social Sciences - Psychology"}: its levels are tried from the most specific up, by
     * English name. {@code broader} tells the match is an ancestor of the labelled field.
     *
     * @return the match, or {@code null} when no level names a known research area
     */
    public ResearchAreaMatch researchArea(String label) {
        if (Objects.isNull(label) || label.isBlank()) {
            return null;
        }

        var index = researchAreaIndex();

        if (index.isEmpty()) {
            // Vocabulary not loaded yet - no match, and nothing remembered for a later call
            return null;
        }

        return researchAreaMatches
            .computeIfAbsent(label.trim(),
                key -> Optional.ofNullable(matchResearchArea(key, index)))
            .orElse(null);
    }

    /**
     * Research area ids for curriculum classifications, in source order and without duplicates.
     * Unmatched classifications and those mapped to a broader area are reported through
     * {@code dropped}.
     */
    public Set<Integer> researchAreaIds(
        Collection<HydratorCVModel.ResearchClassification> classifications,
        Consumer<String> dropped) {
        var researchAreas = new LinkedHashSet<Integer>();

        classifications.stream()
            .filter(classification -> Objects.nonNull(classification) &&
                Objects.nonNull(classification.value()) && !classification.value().isBlank())
            .forEach(classification -> {
                var match = researchArea(classification.value());

                if (Objects.isNull(match)) {
                    dropped.accept("research classification '" + classification.code() +
                        "' (" + classification.value() + ") has no research area");
                    return;
                }

                if (match.broader()) {
                    dropped.accept("research classification '" + classification.value() +
                        "' mapped to broader area '" + match.name() + "'");
                }

                researchAreas.add(match.id());
            });

        return researchAreas;
    }

    public void clearResearchAreaCache() {
        researchAreaIdsByName = null;
        researchAreaMatches.clear();
    }

    private ResearchAreaMatch matchResearchArea(String label, Map<String, Integer> index) {
        var levels = label.split(RESEARCH_AREA_LEVEL_SEPARATOR);

        for (int level = levels.length - 1; level >= 0; level--) {
            var id = index.get(researchAreaKey(levels[level]));

            if (Objects.nonNull(id)) {
                return new ResearchAreaMatch(id, levels[level].trim(),
                    level < levels.length - 1);
            }
        }

        return null;
    }

    private Map<String, Integer> researchAreaIndex() {
        var index = researchAreaIdsByName;

        if (Objects.isNull(index)) {
            synchronized (this) {
                index = researchAreaIdsByName;

                if (Objects.isNull(index)) {
                    var loaded = new HashMap<String, Integer>();
                    researchAreaService.getResearchAreaNames(LanguageAbbreviations.ENGLISH)
                        .forEach(name -> loaded.putIfAbsent(researchAreaKey(name.b), name.a));

                    if (loaded.isEmpty()) {
                        return Map.of();
                    }

                    index = Map.copyOf(loaded);
                    researchAreaIdsByName = index;
                }
            }
        }

        return index;
    }

    private String researchAreaKey(String name) {
        var key = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFKD))
            .replaceAll("")
            .toLowerCase(Locale.ROOT)
            .replace("&", "and")
            .replaceAll("\\s+", " ")
            .trim();

        return RESEARCH_AREA_SYNONYMS.getOrDefault(key, key);
    }

    public record ResearchAreaMatch(Integer id, String name, boolean broader) {
    }

    /**
     * A date without a year is treated as absent and yields {@code null}. Unlike
     * {@link #localDate}, an impossible month or day does not fail the item: the year alone is
     * still a usable date, so a month outside 1-12 drops the month and the day, and a day the month
     * does not have drops the day. Callers compare the result with the input to log what was lost.
     */
    public FlexibleDateDTO flexibleDate(HydratorCVModel.DateInfo dateInfo) {
        if (Objects.isNull(dateInfo)) {
            return null;
        }

        var year = parseInteger(dateInfo.year());

        if (Objects.isNull(year)) {
            return null;
        }

        var month = parseInteger(dateInfo.month());

        if (Objects.isNull(month) || month < 1 || month > 12) {
            return new FlexibleDateDTO(year, null, null, null);
        }

        var day = parseInteger(dateInfo.day());

        if (Objects.nonNull(day) && !YearMonth.of(year, month).isValidDay(day)) {
            day = null;
        }

        return new FlexibleDateDTO(year, month, day, null);
    }

    /**
     * A date without a year is treated as absent and yields {@code null}; a missing month or day
     * defaults to 1. A value that is present but cannot form a date (unparsable part, day without
     * month, month 13, February 30th...) is rejected rather than guessed, so the item fails and the
     * source can be corrected.
     *
     * @throws InvalidSourceValueException when the date is present but invalid
     */
    public LocalDate localDate(HydratorCVModel.DateInfo dateInfo) {
        if (Objects.isNull(dateInfo) || isBlank(dateInfo.year())) {
            return null;
        }

        var year = parseInteger(dateInfo.year());
        var month = parseInteger(dateInfo.month());
        var day = parseInteger(dateInfo.day());

        if (Objects.isNull(year) ||
            (!isBlank(dateInfo.month()) && Objects.isNull(month)) ||
            (!isBlank(dateInfo.day()) && Objects.isNull(day)) ||
            (Objects.isNull(month) && Objects.nonNull(day))) {
            throw invalidDate(dateInfo);
        }

        try {
            return LocalDate.of(year, Objects.requireNonNullElse(month, 1),
                Objects.requireNonNullElse(day, 1));
        } catch (DateTimeException e) {
            throw invalidDate(dateInfo);
        }
    }

    private InvalidSourceValueException invalidDate(HydratorCVModel.DateInfo dateInfo) {
        return new InvalidSourceValueException(String.format(
            "invalid date (year='%s', month='%s', day='%s')",
            dateInfo.year(), dateInfo.month(), dateInfo.day()));
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    public Integer parseInteger(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Institutions in the curriculum payload carry no identifier, so their identity has to be
     * synthesised from the name. See the open question in the design document - this is the weakest
     * link in the pipeline and should be validated against real data.
     */
    public String institutionKey(HydratorCVModel.Institution institution) {
        if (Objects.isNull(institution) || Objects.isNull(institution.name())) {
            return null;
        }

        return normalise(institution.name());
    }

    public String normalise(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Citation names come as {@code "Surname, Given"} or, without a comma, as
     * {@code "Surname INITIALS"}; other single-string forms put the surname last.
     */
    public PersonNameDTO splitName(String value, PersonNameType type) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }

        var trimmed = value.trim().replaceAll("\\s+", " ");
        var comma = trimmed.indexOf(',');

        if (comma > 0 && comma < trimmed.length() - 1) {
            return personName(trimmed.substring(comma + 1).trim(),
                trimmed.substring(0, comma).trim(), type);
        }

        var words = trimmed.split(" ");
        var initialsStart = trailingInitialsStart(words, trimmed);

        if (initialsStart > 0 && initialsStart < words.length) {
            return personName(String.join(" ", Arrays.copyOfRange(words, initialsStart,
                    words.length)),
                String.join(" ", Arrays.copyOfRange(words, 0, initialsStart)), type);
        }

        var lastSpace = trimmed.lastIndexOf(' ');

        if (lastSpace < 0) {
            return personName(trimmed, trimmed, type);
        }

        return personName(trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1),
            type);
    }

    /**
     * Index of the first word in the run of initials ending the name, or {@code words.length}
     * when there is none. In an all-uppercase name a short surname ("DE SÁ", "LAP HO") looks like
     * initials, so there only single letters count.
     */
    private int trailingInitialsStart(String[] words, String name) {
        var allUppercase = name.equals(name.toUpperCase(Locale.ROOT));
        var start = words.length;

        while (start > 0) {
            var word = words[start - 1].replace(".", "");

            if (!INITIALS.matcher(word).matches() || (allUppercase && word.length() > 1)) {
                break;
            }

            start--;
        }

        return start;
    }

    /**
     * Loose identity of a person name across the forms a curriculum uses: the surname without
     * diacritics, punctuation or leading particles, plus the initial of the given names. "Lemos,
     * R.T." and "Ricardo Teixeira Lemos" share the key {@code lemos|r}, "da Rocha, R.P." and
     * "Rosmeri Rocha" share {@code rocha|r}. Null when there is no surname.
     */
    public String authorMatchKey(PersonNameDTO name) {
        if (Objects.isNull(name) || Objects.isNull(name.getLastname())) {
            return null;
        }

        var surnameWords = new ArrayList<>(Arrays.stream(
                foldCase(name.getLastname()).split("[\\s\\-]+"))
            .map(word -> word.replaceAll("[^\\p{L}]", ""))
            .filter(word -> !word.isEmpty())
            .toList());

        while (surnameWords.size() > 1 && SURNAME_PARTICLES.contains(surnameWords.getFirst())) {
            surnameWords.removeFirst();
        }

        if (surnameWords.isEmpty()) {
            return null;
        }

        var givenNames = foldCase(Objects.toString(name.getFirstname(), ""))
            .replaceAll("[^\\p{L}]", "");

        return String.join("", surnameWords) + "|" +
            (givenNames.isEmpty() ? "" : givenNames.substring(0, 1));
    }

    private String foldCase(String value) {
        return DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFKD))
            .replaceAll("")
            .toLowerCase(Locale.ROOT);
    }

    public PersonNameDTO personName(String firstname, String lastname, PersonNameType type) {
        var name = new PersonNameDTO();
        name.setFirstname(firstname);
        // The source never splits out a middle name; core compares contributions on it unguarded
        name.setOtherName("");
        name.setLastname(lastname);
        name.setPersonNameType(type);

        return name;
    }

    private String resolveLanguageTagValue(String language) {
        if (Objects.isNull(language) || language.isBlank()) {
            return LanguageAbbreviations.ENGLISH;
        }

        // Curricula carry locales such as "pt_PT" / "en_GB"; tags are the bare language code
        return language.trim().split("[_-]")[0].toUpperCase(Locale.ROOT);
    }
}
