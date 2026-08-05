package rs.teslaris.migrator.converter.hydrator;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.service.interfaces.document.JournalService;

/**
 * A journal publication needs a journal id, but the curriculum payload only carries the journal
 * name. Journals are therefore created on demand as stubs and cached for the duration of the run.
 * <p>
 * Names are matched case-insensitively after normalisation only - no fuzzy matching. Merging stubs
 * with real journals is left to the existing deduplication tooling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HydratorJournalResolver {

    private final JournalService journalService;

    private final HydratorConversionUtil conversionUtil;

    private final Map<String, Integer> resolvedJournals = new ConcurrentHashMap<>();


    public Integer resolveOrCreate(String journalName, String language) {
        if (Objects.isNull(journalName) || journalName.isBlank()) {
            return null;
        }

        var key = conversionUtil.normalise(journalName);

        return resolvedJournals.computeIfAbsent(key, ignored -> {
            var creationDTO = new JournalBasicAdditionDTO();
            creationDTO.setTitle(conversionUtil.multilingualContent(journalName, language));

            var created = journalService.createJournal(creationDTO);
            log.info("Created stub journal '{}' (id={}) during migration.", journalName,
                created.getId());

            return created.getId();
        });
    }

    public void clearCache() {
        resolvedJournals.clear();
    }
}
