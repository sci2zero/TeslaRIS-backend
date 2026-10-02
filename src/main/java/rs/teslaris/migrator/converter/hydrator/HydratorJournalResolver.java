package rs.teslaris.migrator.converter.hydrator;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * A journal publication needs a journal id, but the curriculum payload only carries the journal
 * name and, sometimes, its ISSN as a "Part of" identifier. Journals are therefore created on
 * demand as stubs and cached.
 * <p>
 * The ISSN is the stronger key: an existing journal with it is reused, even one created by an
 * earlier run. Names are matched case-insensitively after normalisation only - no fuzzy matching.
 * An article with neither is linked to a shared "Not informed" journal (MAP-000053). Merging stubs
 * with real journals is left to the existing deduplication tooling.
 * <p>
 * The cache only ever holds journals that exist, so unlike a cache of misses it may outlive a run.
 */
@Component
@RequiredArgsConstructor
public class HydratorJournalResolver {

    private static final String NOT_INFORMED = "Not informed";

    private static final String JOURNAL_ENTITY = "JOURNAL";

    private final JournalService journalService;

    private final MigrationLog migrationLog;

    private final PublicationSeriesRepository publicationSeriesRepository;

    private final HydratorConversionUtil conversionUtil;

    private final Map<String, Integer> resolvedJournals = new ConcurrentHashMap<>();


    public Integer resolveOrCreate(String journalName, String issn, String language) {
        var title = isBlank(journalName) ? NOT_INFORMED : journalName;

        if (!isBlank(issn)) {
            var normalisedIssn = issn.trim().toUpperCase(Locale.ROOT);

            return resolvedJournals.computeIfAbsent("issn:" + normalisedIssn,
                key -> findByIssn(normalisedIssn)
                    .orElseGet(() -> create(key, title, normalisedIssn, language)));
        }

        return resolvedJournals.computeIfAbsent("name:" + conversionUtil.normalise(title),
            key -> create(key, title, null, language));
    }

    public void clearCache() {
        resolvedJournals.clear();
    }

    private Optional<Integer> findByIssn(String issn) {
        return publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN(issn, issn)
            .stream()
            .filter(Journal.class::isInstance)
            .map(PublicationSeries::getId)
            .findFirst();
    }

    private Integer create(String key, String title, String issn, String language) {
        var creationDTO = new JournalBasicAdditionDTO();
        creationDTO.setTitle(conversionUtil.multilingualContent(title, language));
        creationDTO.setPrintISSN(issn);

        var created = journalService.createJournal(creationDTO);
        migrationLog.stubCreated(HydratorSource.NAME, JOURNAL_ENTITY, key, created.getId(),
            title);

        return created.getId();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
