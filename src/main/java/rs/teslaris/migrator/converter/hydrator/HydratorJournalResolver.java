package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * A journal publication needs a journal id, but the curriculum payload only carries the journal
 * name and, sometimes, its ISSN as a "Part of" identifier. Journals are therefore created on
 * demand as stubs and cached.
 * <p>
 * The ISSN is the stronger key: an existing journal with it is reused, even one created by an
 * earlier run. ISSNs are read in the NNNN-NNNC form, at most two per value (print, then
 * electronic). Names are matched case-insensitively after normalisation only - no fuzzy matching,
 * and only within the run. A journal found by name that has no ISSN yet takes the article's ISSNs;
 * one holding other ISSNs is a different journal. An article with neither is linked to a shared
 * "Not informed" journal (CW-001-MAP-000053).
 * <p>
 * The cache holds only journals that exist, but deduplication between runs may merge or remove
 * them, so it is cleared per run like every other resolver cache.
 */
@Component
@RequiredArgsConstructor
public class HydratorJournalResolver implements RunScopedCache {

    private static final String NOT_INFORMED = "Not informed";

    private static final String JOURNAL_ENTITY = "JOURNAL";

    private static final Pattern ISSN =
        Pattern.compile("(?<![\\dX])(\\d{4})-?(\\d{3}[\\dX])(?![\\dX])");

    private final JournalService journalService;

    private final MigrationLog migrationLog;

    private final PublicationSeriesRepository publicationSeriesRepository;

    private final HydratorConversionUtil conversionUtil;

    private final HydratorJournalIssnWriter issnWriter;

    private final Map<String, Integer> resolvedJournals = new HashMap<>();

    private final Set<Integer> journalsWithIssn = new HashSet<>();


    public synchronized Integer resolveOrCreate(String journalName, String issnValue,
                                                String language, String sourceKey) {
        var informed = !isBlank(journalName);
        var title = informed ? journalName : NOT_INFORMED;
        var issns = issns(issnValue, sourceKey);
        var nameKey = "name:" + conversionUtil.normalise(title);

        var byIssn = findByIssns(issns);
        if (byIssn.isPresent()) {
            remember(byIssn.get(), informed ? nameKey : null, issns);
            return byIssn.get();
        }

        var byName = informed || issns.isEmpty() ? resolvedJournals.get(nameKey) : null;
        if (Objects.nonNull(byName)) {
            if (issns.isEmpty()) {
                return byName;
            }

            if (!journalsWithIssn.contains(byName)) {
                issnWriter.fill(byName, issns);
                remember(byName, nameKey, issns);
                return byName;
            }
        }

        var created = create(title, issns, language);
        remember(created, informed || issns.isEmpty() ? nameKey : null, issns);

        return created;
    }

    @Override
    public synchronized void clearCache() {
        resolvedJournals.clear();
        journalsWithIssn.clear();
    }

    private List<String> issns(String value, String sourceKey) {
        var issns = new ArrayList<String>();

        if (isBlank(value)) {
            return issns;
        }

        var matcher = ISSN.matcher(value.toUpperCase());
        while (matcher.find()) {
            var issn = matcher.group(1) + "-" + matcher.group(2);

            if (!validCheckDigit(issn)) {
                dropIssn(sourceKey, "invalid ISSN check digit: '" + issn + "'");
            } else if (issns.size() == 2) {
                dropIssn(sourceKey, "more than two ISSNs, '" + issn + "' not kept");
            } else if (!issns.contains(issn)) {
                issns.add(issn);
            }
        }

        if (issns.isEmpty()) {
            dropIssn(sourceKey, "invalid ISSN format: '" + value + "'");
        }

        return issns;
    }

    private boolean validCheckDigit(String issn) {
        var digits = issn.replace("-", "");
        var sum = 0;

        for (int i = 0; i < 7; i++) {
            sum += (digits.charAt(i) - '0') * (8 - i);
        }

        var check = (11 - sum % 11) % 11;
        var expected = check == 10 ? 'X' : (char) ('0' + check);

        return digits.charAt(7) == expected;
    }

    private void dropIssn(String sourceKey, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.JOURNAL_PUBLICATION.name(), sourceKey,
            HydratorMappingRule.cv(67), reason);
    }

    private Optional<Integer> findByIssns(List<String> issns) {
        for (var issn : issns) {
            var cached = resolvedJournals.get("issn:" + issn);
            if (Objects.nonNull(cached)) {
                return Optional.of(cached);
            }
        }

        for (var issn : issns) {
            var stored = publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN(issn,
                    issn)
                .stream()
                .filter(Journal.class::isInstance)
                .map(PublicationSeries::getId)
                .findFirst();
            if (stored.isPresent()) {
                return stored;
            }
        }

        return Optional.empty();
    }

    private void remember(Integer journalId, String nameKey, List<String> issns) {
        if (Objects.nonNull(nameKey)) {
            resolvedJournals.putIfAbsent(nameKey, journalId);
        }

        issns.forEach(issn -> resolvedJournals.putIfAbsent("issn:" + issn, journalId));

        if (!issns.isEmpty()) {
            journalsWithIssn.add(journalId);
        }
    }

    private Integer create(String title, List<String> issns, String language) {
        var creationDTO = new JournalBasicAdditionDTO();
        creationDTO.setTitle(conversionUtil.multilingualContent(title, language));

        if (!issns.isEmpty()) {
            creationDTO.setPrintISSN(issns.getFirst());
        }
        if (issns.size() > 1) {
            creationDTO.setEISSN(issns.get(1));
        }

        var key = issns.isEmpty() ?
            "name:" + conversionUtil.normalise(title) : "issn:" + issns.getFirst();
        var created = journalService.createJournal(creationDTO);
        migrationLog.stubCreated(HydratorSource.NAME, JOURNAL_ENTITY, key, created.getId(), title);

        return created.getId();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
